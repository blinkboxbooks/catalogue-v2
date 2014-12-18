package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.Events.Undistribute

object UndistributeFixtures {
  val simple = Undistribute(
    isbn = "1234567890123",
    sequenceNumber = 101,
    usable = true,
    reasons = List.empty
  )

  def simpleWith(isbn: String) =
    simple.copy(isbn = isbn)
}
