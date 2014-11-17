package com.blinkbox.books.catalogue.ingester.messaging

import java.net.ConnectException

import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.search.Indexer
import com.blinkbox.books.catalogue.common.{Book, DistributeContent}
import com.blinkbox.books.catalogue.ingester.parser.IngestionParser
import com.blinkbox.books.messaging.{ErrorHandler, Event, ReliableEventHandler}
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.transport.RemoteTransportException

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class V1MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                       indexer: Indexer, messageParser: IngestionParser[String, DistributeContent])
  extends ReliableEventHandler(errorHandler, retryInterval) with V1EventToBook {

  override protected[this] def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    Future.fromTry(toBook(event, messageParser)).flatMap(index)

  override protected[this] def isTemporaryFailure(e: Throwable): Boolean =
    e.isInstanceOf[ConnectException]


  private def index(book: Book): Future[Unit] = {
    val indexing = indexer.index(book)
    indexing.onFailure{
      case e: VersionConflictEngineException =>
        log.error(s"CONFLICT: ${e.getMessage} - modifiedAt[${book.modifiedAt}]")
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] =>
        log.error(s"CONFLICT: ${e.getCause.getMessage} - modifiedAt[${book.modifiedAt}]")
    }
    indexing.recover{
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] => ()
    }.map(_ => ())
  }
}
