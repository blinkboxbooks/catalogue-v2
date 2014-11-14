package com.blinkbox.books.agora.catalogue.book

import com.blinkbox.books.spray.v1.{Image, Link}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class BookRepresentation(guid: String, id: String, title: String, publicationDate: String, sampleEligible: Boolean, images: List[Image], links: Option[List[Link]])

// TODO - should this (and any other v1 model classes) be in an explicit v1 package?

/*
object Book {
  val fmt = DateTimeFormat.forPattern("yyyy-MM-dd")
  def apply(isbn: String, title: String, publicationDate: DateTime, sampleEligible: Boolean, images: List[Image], links: Option[List[Link]]) = {
    println("*****************************")
    new Book(s"urn:blinkboxbooks:id:book:$isbn", isbn, title, fmt.print(publicationDate), sampleEligible, images, links)
  }
}
*/
