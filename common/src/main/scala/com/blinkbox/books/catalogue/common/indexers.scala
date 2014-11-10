package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.Indexer.IndexContent

class BookIndexContent extends IndexContent[Book] {
  type IdType = String

  override def path: String = "catalogue/book"

  override def fields(content: Book): Map[String, Any] = ???

  override def id(content: Book): Option[IdType] = Option(content.isbn)
}
