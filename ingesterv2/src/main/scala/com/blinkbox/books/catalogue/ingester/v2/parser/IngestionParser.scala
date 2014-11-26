package com.blinkbox.books.catalogue.ingester.v2.parser

import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common._
import com.blinkbox.books.messaging.EventBody
import org.json4s.jackson.Serialization
import scala.util.{Success, Failure, Try}

trait IngestionParser[T, R] {
  def parse(content: T): Try[R]
}

class JsonV2IngestionParser extends IngestionParser[EventBody,Book] {
  import Json.formats

  override def parse(content: EventBody): Try[Book] =
    Try(Serialization.read[Book](new String(content.content, "UTF-8")))
      .transform(
        book => Success(book),
        e => Failure(new RuntimeException("Not able to parse json", e)))
}
