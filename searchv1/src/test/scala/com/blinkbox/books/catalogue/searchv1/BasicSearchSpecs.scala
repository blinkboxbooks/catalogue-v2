package com.blinkbox.books.catalogue.searchv1

import V1SearchService.Book
import com.blinkbox.books.catalogue.common.BookFixtures
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class BasicSearchSpecs extends FlatSpec with Matchers with ApiSpecBase {

  val simpleBookResponse = Book("0000000000000", "A simple book", "Foo C. Bar" :: Nil) :: Nil

  "The search API" should "retrieve empty result-sets from an empty index" in {
    e2e createIndex catalogue andAfter { _ =>
      Get("/catalogue/search/books?q=Foo") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(Nil)
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the title" in {
    e2e createIndex catalogue indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=simple") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(simpleBookResponse)
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the author" in {
    e2e createIndex catalogue indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=foo") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(simpleBookResponse)
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the content" in {
    e2e createIndex catalogue indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=description") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(simpleBookResponse)
      }
    }
  }

  it should "retrieve an empty result set if the given query do not match any field" in {
    e2e createIndex catalogue indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=foobar") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(Nil)
      }
    }
  }

  it should "fail with a 400 (Bad Request) if the 'q' parameter is not provided" in {
    e2e createIndex catalogue andAfter { _ =>
      Get("/catalogue/search/books") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
      }
    }
  }
}
