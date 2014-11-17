package com.blinkbox.books.catalogue.ingester.messaging

import com.blinkbox.books.catalogue.common.{Book, DistributeContent, Undistribute}
import com.blinkbox.books.catalogue.ingester.parser.IngestionParser
import com.blinkbox.books.messaging.Event

import scala.util.Try

trait V1EventToBook {

  def toBook(event: String, messageParser: IngestionParser[String, DistributeContent]): Try[Book] = {
    messageParser.parse(event).map {
      case book: Book => book
      case undistribute: Undistribute => Book.empty.copy(
        isbn = undistribute.isbn,
        distribute = false,
        modifiedAt = undistribute.effectiveTimestamp)
    }
  }

  def toBook(event: Event, messageParser: IngestionParser[String, DistributeContent]): Try[Book] = {
    toBook(new String(event.body.content, "UTF-8"), messageParser)
  }
}
