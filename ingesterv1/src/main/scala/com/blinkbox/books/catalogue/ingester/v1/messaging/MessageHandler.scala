package com.blinkbox.books.catalogue.ingester.v1.messaging

import java.net.ConnectException
import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.Events.{BookPrice, Undistribute, Book}
import com.blinkbox.books.catalogue.common.search.Indexer
import com.blinkbox.books.catalogue.common.{DistributionStatus, DistributeContent}
import com.blinkbox.books.catalogue.ingester.v1.parser.IngestionParser
import com.blinkbox.books.messaging.{ReliableEventHandler, ErrorHandler, Event}
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.transport.RemoteTransportException
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                     indexer: Indexer, messageParser: IngestionParser[String, DistributeContent])
  extends ReliableEventHandler(errorHandler, retryInterval) {

  override protected[this] def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    Future.fromTry(messageParser.parse(new String(event.body.content, "UTF-8"))).flatMap(index)

  override protected[this] def isTemporaryFailure(e: Throwable): Boolean =
    e.isInstanceOf[ConnectException]

  private def index(content: DistributeContent): Future[Unit] = {
    val indexing = content match {
      case book: Book => indexer.index(book)
      case undistribute: Undistribute => indexer.index(undistribute)
      case bookPrice: BookPrice => indexer.index(bookPrice)
    }
    indexing.onFailure{
      case e: VersionConflictEngineException =>
        log.error(s"CONFLICT: ${e.getMessage}")
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] =>
        log.error(s"CONFLICT: ${e.getCause.getMessage}")
    }
    indexing.recover{
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] => ()
    }.map(_ => ())
  }
}
