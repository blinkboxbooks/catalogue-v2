package com.blinkbox.books.agora.catalogue.app

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.{DiagnosticExecutionContext, Loggers}
import com.blinkbox.books.spray._
import spray.can.Http
import spray.http.AllOrigins
import spray.http.HttpHeaders.`Access-Control-Allow-Origin`
import spray.http.Uri.Path
import spray.routing._
import com.typesafe.config.Config
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.agora.catalogue.contributor._
import com.blinkbox.books.agora.catalogue.book._
import scala.concurrent.duration._
import com.blinkbox.books.catalogue.common.ElasticFactory
import spray.http.Uri

class WebService(config: AppConfig) extends HttpServiceActor {
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  val dao = new ElasticBookDao(ElasticFactory.remote(config.elastic), config.elastic.indexName + "/book")

  val linkHelper = LinkHelper(config)
  val bookApi = new BookApi(config.service, config.book, new DefaultBookService(dao, linkHelper))
  val contributorApi = new ContributorApi(config.service, config.contributor, new ElasticSearchContributorService)

  val routes = respondWithHeader(`Access-Control-Allow-Origin`(AllOrigins)) {
    // priceApi.routes ~ synopsisApi.routes ~ publisherApi.routes ~ contributorApi.routes ~ contributorGroupApi.routes ~ categoryApi.routes
    bookApi.routes ~ contributorApi.routes 
  }

  private val that = this
  val healthService = new HealthCheckHttpService {
    override implicit def actorRefFactory = that.actorRefFactory
    override val basePath = Path("/")
  }
  
  def receive = runRoute(routes ~ healthService.routes)
}

object WebApp extends App with Configuration with Loggers {
  val appConfig = AppConfig(config)
  implicit val system = ActorSystem("akka-spray", config)
  implicit val executionContext = DiagnosticExecutionContext(system.dispatcher)
  implicit val timeout = Timeout(appConfig.service.timeout)
  sys.addShutdownHook(system.shutdown())
  val service = system.actorOf(Props(classOf[WebService], appConfig))
  val localUrl = appConfig.service.localUrl
  HttpServer(Http.Bind(service, localUrl.getHost, port = localUrl.effectivePort))
}
