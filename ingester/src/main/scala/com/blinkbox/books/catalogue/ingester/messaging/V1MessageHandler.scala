package com.blinkbox.books.catalogue.ingester.messaging

import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.{EsIndexerTypes, Indexer, Book}
import com.blinkbox.books.catalogue.ingester.parser.IngestionParser
import com.blinkbox.books.messaging.{ErrorHandler, Event, ReliableEventHandler}
import org.json4s.DefaultFormats
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

class V1MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                       indexer: Indexer[EsIndexerTypes], messageParser: IngestionParser[String, Book])
  extends ReliableEventHandler(errorHandler, retryInterval) {

  import com.blinkbox.books.catalogue.common.Indexers._
  implicit private val formats = DefaultFormats

  override protected[this] def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] =
    toBook(event) match {
      case Success(book) =>
        index(book)
      case Failure(e) =>
        Future.failed(e)
    }

  override protected[this] def isTemporaryFailure(exception: Throwable): Boolean =
    // TODO: This needs to be implemented when having the search/index functionality in place
    false

  private def toBook(event: Event): Try[Book] =
    messageParser.parse(new String(event.body.content, "UTF-8"))

  private def index(book: Book): Future[Unit] =
    indexer.index(book).map(_ => ())
}
