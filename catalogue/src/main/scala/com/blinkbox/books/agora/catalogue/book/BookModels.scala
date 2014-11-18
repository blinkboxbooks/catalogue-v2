package com.blinkbox.books.agora.catalogue.book

import com.blinkbox.books.spray.v1.{Image, Link}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class BookRepresentation(guid: String, id: String, title: String, publicationDate: String, sampleEligible: Boolean, images: List[Image], links: Option[List[Link]])
case class BookSynopsis(guid: String, id: String, text: String)

// TODO - should this (and any other v1 model classes) be in an explicit v1 package?

object BookRepresentation {
  val fmt = DateTimeFormat.forPattern("yyyy-MM-dd")
  def apply(isbn: String, title: String, publicationDate: DateTime, sampleEligible: Boolean, images: List[Image], links: Option[List[Link]]) = {
    new BookRepresentation(s"urn:blinkboxbooks:id:book:$isbn", isbn, title, fmt.print(publicationDate), sampleEligible, images, links)
  }
}

object BookSynopsis {
  def apply(id: String, text: String) = new BookSynopsis(s"urn:blinkboxbooks:id:synopsis:$id", id, text)
}

