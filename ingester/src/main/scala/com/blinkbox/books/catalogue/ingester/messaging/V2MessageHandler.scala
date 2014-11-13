package com.blinkbox.books.catalogue.ingester.messaging

import java.net.ConnectException

import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.search.Indexer
import com.blinkbox.books.catalogue.common.Book
import com.blinkbox.books.catalogue.ingester.parser.IngestionParser
import com.blinkbox.books.messaging._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Failure, Try}

class V2MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                       indexer: Indexer, messageParser: IngestionParser[EventBody, Book])
  extends ReliableEventHandler(errorHandler, retryInterval) {

  override protected[this] def handleEvent(event: Event, originalSender: ActorRef): Future[Unit] = {
    toBook(event) match {
      case Success(book) =>
        index(book)
      case Failure(e) =>
        Future.failed(e)
    }
  }

  override protected[this] def isTemporaryFailure(e: Throwable): Boolean =
    e.isInstanceOf[ConnectException]

  private def toBook(event: Event): Try[Book] = {
    messageParser.parse(event.body)
  }

  private def index(book: Book): Future[Unit] =
    indexer.index(book).map(_ => ())
}
