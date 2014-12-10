package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.{UndistributeFixtures, BookFixtures}
import com.blinkbox.books.catalogue.searchv1.V1SearchService.{Book, BookSearchResponse}
import org.scalatest.{Matchers, FlatSpec}
import spray.http.StatusCodes

class UndistributeBookSpecs extends FlatSpec with Matchers with ApiSpecBase {

  private val ISBN = "1234567890123"
  private val Title = "TheTitle"
  private val book = BookFixtures.simpleBook.copy(isbn = ISBN, title = Title)

  private def queryAndCheck[T](q: String)(f: => T) =
    Get(s"/catalogue/search/books?q=$q") ~> routes ~> check(f)

  "The search API" should "retrieve a simple book if it wasn't undistributed" in {
    catalogueIndex indexAndCheck book andAfter { _ =>
      queryAndCheck(Title) {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse].numberOfResults should equal(1)
      }
    }
  }

  "The search API" should "retrieve no undistributed books" in {
    val undistribute = UndistributeFixtures.simpleWith(isbn = ISBN).copy(usable = false)
    catalogueIndex indexAndCheck(book, undistribute) andAfter { _ =>
      queryAndCheck(Title) {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse].numberOfResults should equal(0)
      }
    }
  }
}
