package com.blinkbox.books.catalogue.ingester.v1.messaging

import java.net.ConnectException
import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.search.Indexer
import com.blinkbox.books.catalogue.common.{DistributionStatus, Undistribute, Book, DistributeContent}
import com.blinkbox.books.catalogue.ingester.v1.parser.IngestionParser
import com.blinkbox.books.messaging.{ReliableEventHandler, ErrorHandler, Event}
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.transport.RemoteTransportException
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

trait EventToBook {

  def toBook(event: String, messageParser: IngestionParser[String, DistributeContent]): Try[Book] = {
    messageParser.parse(event).map {
      case book: Book => book
      case undistribute: Undistribute => Book.empty.copy(
        isbn = undistribute.isbn,
        distributionStatus = DistributionStatus(
          usable = false,
          reasons = List("V1 undistribute.")
        ),
        sequenceNumber = undistribute.effectiveTimestamp.getMillis)
    }
  }

  def toBook(event: Event, messageParser: IngestionParser[String, DistributeContent]): Try[Book] = {
    toBook(new String(event.body.content, "UTF-8"), messageParser)
  }
}

class MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                     indexer: Indexer, messageParser: IngestionParser[String, DistributeContent])
  extends ReliableEventHandler(errorHandler, retryInterval) with EventToBook {

  override protected[this] def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    Future.fromTry(toBook(event, messageParser)).flatMap(index)

  override protected[this] def isTemporaryFailure(e: Throwable): Boolean =
    e.isInstanceOf[ConnectException]


  private def index(book: Book): Future[Unit] = {
    val indexing = indexer.index(book)
    indexing.onFailure{
      case e: VersionConflictEngineException =>
        log.error(s"CONFLICT: ${e.getMessage} - sequenceNumber[${book.sequenceNumber}]")
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] =>
        log.error(s"CONFLICT: ${e.getCause.getMessage} - sequenceNumber[${book.sequenceNumber}]")
    }
    indexing.recover{
      case e: RemoteTransportException if e.getCause.isInstanceOf[VersionConflictEngineException] => ()
    }.map(_ => ())
  }
}
