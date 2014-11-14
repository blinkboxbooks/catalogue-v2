package com.blinkbox.books.catalogue.ingester.messaging

import java.net.ConnectException

import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.{DistributeContent, Undistribute, Book}
import com.blinkbox.books.catalogue.common.search.{Search, Indexer}
import com.blinkbox.books.catalogue.ingester.parser.IngestionParser
import com.blinkbox.books.messaging.{ErrorHandler, Event, ReliableEventHandler}
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.transport.RemoteTransportException
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class V1MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                       indexer: Indexer, messageParser: IngestionParser[String, DistributeContent])
  extends ReliableEventHandler(errorHandler, retryInterval) {

  override protected[this] def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] = {
    for {
      book <- toBook(event)
      _ <- index(book)
    } yield ()
  }

  override protected[this] def isTemporaryFailure(e: Throwable): Boolean =
    e.isInstanceOf[ConnectException]

  private def toBook(event: Event): Future[Book] = {
    messageParser.parse(new String(event.body.content, "UTF-8")) match {
      case Success(book: Book) =>
        Future.successful(book)
      case Success(undistribute: Undistribute) =>
        Future.successful(Book.empty.copy(
          isbn = undistribute.isbn,
          distribute = false,
          modifiedAt = undistribute.effectiveTimestamp))
      case Failure(e) =>
        Future.failed(e)
    }
  }

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
