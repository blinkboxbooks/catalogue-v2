package com.blinkbox.books.catalogue.ingester.messaging

import java.net.ConnectException

import akka.actor.ActorRef
import com.blinkbox.books.catalogue.common.{Undistribute, Book}
import com.blinkbox.books.catalogue.common.search.{Search, Indexer}
import com.blinkbox.books.catalogue.ingester.parser.IngestionParser
import com.blinkbox.books.messaging.{ErrorHandler, Event, ReliableEventHandler}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Either, Failure, Success}

class V1MessageHandler(errorHandler: ErrorHandler, retryInterval: FiniteDuration,
                       indexer: Indexer, search: Search,
                       messageParser: IngestionParser[String, Either[Book, Undistribute]])
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
      case Success(Left(book)) =>
        Future.successful(book)
      case Success(Right(undistribute)) =>
        search.lookup(undistribute.isbn)
          .map { optBook =>
            optBook
              .map(book => book.copy(distribute = false))
              .getOrElse(throw new RuntimeException(s"book not found to undistribute for isbn [${undistribute.isbn}]"))
          }
      case Failure(e) =>
        Future.failed(e)
    }
  }

  private def index(book: Book): Future[Unit] =
    indexer.index(book).map(_ => ())
}
