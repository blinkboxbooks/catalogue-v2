package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.BookFixtures
import com.blinkbox.books.catalogue.searchv1.V1SearchService.BookSearchResponse
import org.scalatest.{Matchers, FlatSpec}
import spray.http.StatusCodes
import scala.concurrent.duration._

class SpecialCharactersSpecs extends FlatSpec with Matchers with ApiSpecBase {

  import ResponseFixtures._

  override def beforeAll(): Unit = {
    super.beforeAll()
    val book = BookFixtures.simpleBook
    catalogueIndex indexAndCheck book andAwaitFor 20.seconds
  }

  def checkInvalidResponse() = super.checkInvalidResponse("Invalid or empty search term")

  "The search API" should "retrieve a simple book if given a query that matches the ISBN removing special characters" in {
    Get("/catalogue/search/books?q=1234567890123%2F") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(simpleBookResponse("1234567890123/"))
    }
  }

  it should "signal an error in case the query is only composed by special characters" in {
    Get("/catalogue/search/books?q=*") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse()
    }
    Get("/catalogue/search/books?q=?") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse()
    }
    Get("/catalogue/search/books?q=$") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse()
    }
    Get("/catalogue/search/books?q=%C2%A3") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse()
    }
  }

  it should "correctly retrieve a book ignoring the ? character if a valid query is provided" in {
    Get("/catalogue/search/books?q=simple?") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(simpleBookResponse("simple?"))
    }
  }
}
