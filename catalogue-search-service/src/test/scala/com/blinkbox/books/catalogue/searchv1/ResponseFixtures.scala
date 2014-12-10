package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.searchv1.V1SearchService.{Book, BookSearchResponse}

object ResponseFixtures {
  def simpleBookResponse(q: String) = BookSearchResponse(q, Some(Book("1234567890123", "A simple book", "Foo C. Bar" :: Nil) :: Nil), 1)
  def emptyBookResponse(q: String) = BookSearchResponse(q, None, 0)
}
