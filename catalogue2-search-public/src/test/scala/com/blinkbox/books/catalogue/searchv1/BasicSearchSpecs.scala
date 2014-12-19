package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.BookFixtures
import com.blinkbox.books.catalogue.searchv1.V1SearchService.BookSearchResponse
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes
import scala.concurrent.duration._

class BasicSearchSpecs extends FlatSpec with Matchers with ApiSpecBase {

  import com.blinkbox.books.catalogue.searchv1.ResponseFixtures._

  override def beforeAll() {

    super.beforeAll()
    catalogueIndex indexAndCheck BookFixtures.simpleBook andAwaitFor(10.seconds)
  }

  "The search API" should "retrieve a simple book if given a query that match in the title" in {
    Get("/catalogue/search/books?q=simple") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(simpleBookResponse("simple"))
    }
  }

  it should "retrieve a simple book if given a query that match in the author" in {
    Get("/catalogue/search/books?q=kilgore") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(simpleBookResponse("kilgore"))
    }
  }

  it should "retrieve a simple book if given a query that match in the content" in {
    Get("/catalogue/search/books?q=description") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(simpleBookResponse("description"))
    }
  }

  it should "retrieve a simple book if given a query that matches the ISBN" in {
    Get("/catalogue/search/books?q=1234567890123") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(simpleBookResponse("1234567890123"))
    }
  }

  it should "retrieve an empty result set if the given query do not match any field" in {
    Get("/catalogue/search/books?q=foobar") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(emptyBookResponse("foobar"))
    }
  }
}
