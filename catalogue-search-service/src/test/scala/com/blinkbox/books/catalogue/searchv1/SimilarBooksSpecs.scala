package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.searchv1.V1SearchService.BookSimilarResponse
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

import scala.concurrent.duration._

class SimilarBooksSpecs extends FlatSpec with Matchers with ApiSpecBase {

  override def beforeAll(): Unit = {
    super.beforeAll()
    populateDummyIndex(100) andAwaitFor (10.seconds)
  }

  "The similar-books endpoint" should "return books that are similar to the provided one" in {
    Get("/catalogue/search/books/0000000000001/similar") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSimilarResponse]
      resp.numberOfResults should equal(99)
    }
  }

  it should "return a 400 signaling an invalid ID if the isbn is not numeric" in {
    Get("/catalogue/search/books/abcdefghijklm/similar") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      responseAs[String] should equal("Invalid ID: abcdefghijklm")
    }
  }

  it should "return a 400 signaling an invalid ID if the isbn is shorter than 13 digits" in {
    Get("/catalogue/search/books/123456789012/similar") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      responseAs[String] should equal("Invalid ID: 123456789012")
    }
  }

  it should "return a 400 signaling an invalid ID if the isbn is longer than 13 digits" in {
    Get("/catalogue/search/books/12345678901234/similar") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      responseAs[String] should equal("Invalid ID: 12345678901234")
    }
  }
}
