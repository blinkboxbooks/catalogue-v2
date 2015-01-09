package com.blinkbox.books.catalogue.ingester.v1.messaging

import java.io.IOException
import java.net.ConnectException
import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.search.{CommunicationException, Indexer}
import com.blinkbox.books.catalogue.common.DistributeContent
import com.blinkbox.books.catalogue.ingester.v1.parser.IngestionParser
import com.blinkbox.books.elasticsearch.client.{RequestException, UnsuccessfulResponse, FailedRequest}
import com.blinkbox.books.messaging.{ReliableEventHandler, ErrorHandler, Event}
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.transport.RemoteTransportException
import spray.can.Http.ConnectionAttemptFailedException
import spray.http.StatusCodes
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                     indexer: Indexer, messageParser: IngestionParser[String, DistributeContent])
  extends ReliableEventHandler(errorHandler, retryInterval) {

  override protected[this] def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    Future.fromTry(messageParser.parse(new String(event.body.content, "UTF-8"))).flatMap(index)

  override protected[this] def isTemporaryFailure(e: Throwable): Boolean =
    e match {
      case (_:ConnectException
           |_:IOException
           |RequestException(_, _: ConnectionAttemptFailedException)) =>
        true
      case _ =>
        false
    }

  private def index(content: DistributeContent): Future[Unit] = {
    val indexing = indexer.index(content)
    indexing.onFailure{
      case UnsuccessfulResponse(StatusCodes.Conflict, msg) =>
        log.error(s"CONFLICT: $msg")
      case e: VersionConflictEngineException =>
        log.error(s"CONFLICT: ${e.getMessage}")
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] =>
        log.error(s"CONFLICT: ${e.getCause.getMessage}")
    }
    indexing.recover{
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] => ()
      case UnsuccessfulResponse(StatusCodes.Conflict, _) => ()
    }.map(_ => ())
  }
}
