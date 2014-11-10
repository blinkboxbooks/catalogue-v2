package com.blinkbox.books.catalogue.common

import com.blinkbox.books.messaging.{JsonEventBody, MediaType}

object Distribute {
  case class Classification(realm: String, id: String)
  case class Availability(available: Boolean, code: String, extra: String)
  case class Description(classification: List[Classification], content: String, `type`: String, author: String)
  case class Subject(`type`: String, code: String)
  case class Uri(`type`: String, uri: String, params: Option[String])
  case class Epub(classification: List[Classification], uris: List[Uri], keyFile: Option[String], wordCount: Long, size: Long)
  case class Image(classification: List[Classification], uris: List[Uri], width: Int, height: Int, size: Int)
  case class Media(epubs: List[Epub])
  case class RegionalRights(`GB`: Option[Boolean], `ROW`: Option[Boolean], `WORLD`: Option[Boolean])
  case class Book(title: String, availability: Availability,  isbn: String,
                  regionalRights: RegionalRights, publisher: String, media: Media,
                  languages: List[String], descriptions: List[Description], subjects: List[Subject])

  val jsonMediaType = MediaType("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json")

  implicit object Book extends JsonEventBody[Book] {
    val jsonMediaType = Distribute.jsonMediaType
  }
}
