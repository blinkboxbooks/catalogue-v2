package com.blinkbox.books.catalogue.ingester

import java.io.File
import java.util.concurrent.{Executors, TimeUnit}
import akka.actor.Status.Success
import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.util.Timeout
import com.blinkbox.books.catalogue.common.search.{EsSearch, Schema, EsIndexer}
import com.blinkbox.books.catalogue.ingester.Main._
import com.blinkbox.books.catalogue.ingester.messaging.{V2MessageHandler, V1MessageHandler}
import com.blinkbox.books.catalogue.ingester.parser.{XmlV1IngestionParser, JsonV2IngestionParser}
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.messaging._
import com.blinkbox.books.rabbitmq.RabbitMqConfirmedPublisher.PublisherConfiguration
import com.blinkbox.books.rabbitmq.RabbitMqConsumer.QueueConfiguration
import com.blinkbox.books.rabbitmq.{RabbitMqConsumer, RabbitMqConfirmedPublisher, RabbitMq, RabbitMqConfig}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s.JsonAST.JObject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.Source
import scala.xml

object Hierarchy {
  def start(actorSystem: ActorSystem): ActorRef =
    actorSystem.actorOf(Props(new IngesterSupervisor), "catalogue-ingester-supervisor")

  def startTestScheduler(actorSystem: ActorSystem): Unit = {
    implicit val msgExecutionCtx = DiagnosticExecutionContext(actorSystem.dispatcher)
    val rabbitmqConfig = RabbitMqConfig(config)
    val publisherConnection = RabbitMq.recoveredConnection(rabbitmqConfig)

    val messagePublisherV2 = actorSystem.actorOf(
      Props(new RabbitMqConfirmedPublisher(
        connection = publisherConnection,
        config = PublisherConfiguration(
          config.getConfig("messageListener.distributor.book.outputv2")))))

    val messagePublisherV1 = actorSystem.actorOf(
      Props(new RabbitMqConfirmedPublisher(
        connection = publisherConnection,
        config = PublisherConfiguration(
          config.getConfig("messageListener.distributor.book.outputv1")))))

    implicit object JObjectJson extends JsonEventBody[JObject] {
      val jsonMediaType = MediaType("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json")
    }
    val json = Event.json(EventHeader("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json"), jvalue)
    actorSystem.scheduler.scheduleOnce(10.seconds){
      messagePublisherV2 ! json
    }
    actorSystem.scheduler.schedule(0.milliseconds, 5.seconds, new Runnable {
      override def run(): Unit = {
        for(i <- 0 to 0) {
          messagePublisherV2 ! json
        }
      }
    })

    actorSystem.scheduler.scheduleOnce(5.seconds){
      val xml =
        """
          <undistribute xmlns="http://schemas.blinkboxbooks.com/distribution/undistribute/v1" xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1" xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning" r:timestamp="2014-11-06T19:14:05Z" r:originator="" v:version="1.0">
          <book ref="isbn">9780373876396</book>
          </undistribute>
        """
      val event = Event.xml(xml, EventHeader("application/vnd.blinkbox.books.ingestion.book.metadata.v1"))
      messagePublisherV1 ! event
    }

//    actorSystem.scheduler.scheduleOnce(5.seconds){
//      val dirName = "/Users/alinp/work/blinkbox/zz.Distribution.Book"
//      val xmlFiles = new File(dirName).listFiles.filter(_.getName.endsWith(".xml")).map(file => s"$dirName/${file.getName}")
//      xmlFiles.foreach{ file =>
//        val event = Event.xml(Source.fromFile(file, "UTF-8").mkString, EventHeader("application/vnd.blinkbox.books.ingestion.book.metadata.v1"))
//        messagePublisherV1 ! event
//      }
//    }
  }
}

class IngesterSupervisor extends Actor with StrictLogging {
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: Exception => Restart
    }

  override def receive: Receive = {
    context.actorOf(Props(new MessagingSupervisor), "messaging-supervisor")
    started()
  }

  private def started(): Receive = {
    case Children => sender ! context.children.toList.map(_.path.toString)
  }
}

class MessagingSupervisor extends Actor with StrictLogging {
  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: Exception => Restart
    }

  override def receive: Receive = {
    startMessaging()
    started()
  }

  private def started(): Receive = {
    case Children =>
      sender ! context.children.toList.map(_.path.toString);
    case Success(_) =>
      ()
    case msg =>
      logger.warn(s"Unknown message [$msg]")
  }

  private def startMessaging(): Unit = {
    implicit val msgExecutionCtx = DiagnosticExecutionContext(context.dispatcher)
    implicit val apiTimeout = Timeout(config.getDuration("messageListener.actorTimeout", TimeUnit.SECONDS).seconds)

    val rabbitmqConfig = RabbitMqConfig(config)
    val consumerConnection = RabbitMq.reliableConnection(rabbitmqConfig)
    val publisherConnection = RabbitMq.recoveredConnection(rabbitmqConfig)

    val errorsPublisher = context.actorOf(
      Props(new RabbitMqConfirmedPublisher(
        connection = publisherConnection,
        config = PublisherConfiguration(
          config.getConfig("messageListener.distributor.book.errors")))))

    val errorHandler = new ActorErrorHandler(errorsPublisher)
    val esClient = ElasticClient.remote(config.getString("search.host"), config.getInt("search.port"))
    val indexingEc = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))
    val indexer = new EsIndexer(config, esClient)(indexingEc)
    val search = new EsSearch(config, esClient)(indexingEc)

    val v1messageConsumer = context.actorOf(
      Props(new RabbitMqConsumer(
        channel = consumerConnection.createChannel(),
        consumerTag = "ingester-books-consumer-v1",
        output = context.actorOf(
          Props(new V1MessageHandler(
            errorHandler = errorHandler,
            retryInterval = 10.seconds,
            indexer = indexer,
            search = search,
            messageParser = new XmlV1IngestionParser))),
        queueConfig = QueueConfiguration(
          config.getConfig("messageListener.distributor.book.inputv1")))), name = "V1-Message-Consumer")

    val v2messageConsumer = context.actorOf(
      Props(new RabbitMqConsumer(
        channel = consumerConnection.createChannel(),
        consumerTag = "ingester-books-consumer-v2",
        output = context.actorOf(
          Props(new V2MessageHandler(
            errorHandler = new ActorErrorHandler(errorsPublisher),
            retryInterval = 10.seconds,
            indexer = indexer,
            messageParser = new JsonV2IngestionParser))),
        queueConfig = QueueConfiguration(
          config.getConfig("messageListener.distributor.book.inputv2")))), name = "V2-Message-Consumer")

    esClient.execute(Schema(config).catalogue).onComplete {
      case _ =>
        v1messageConsumer ! RabbitMqConsumer.Init
        v2messageConsumer ! RabbitMqConsumer.Init
    }
  }
}
