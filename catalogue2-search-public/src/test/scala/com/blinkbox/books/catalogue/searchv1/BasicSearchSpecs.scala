package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.BookFixtures
import com.blinkbox.books.catalogue.searchv1.V1SearchService.BookSearchResponse
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class BasicSearchSpecs extends FlatSpec with Matchers with ApiSpecBase {

  import com.blinkbox.books.catalogue.searchv1.ResponseFixtures._

  "The search API" should "retrieve empty result-sets from an empty index" in {
    catalogueIndex andAfter { _ =>
      Get("/catalogue/search/books?q=Foo") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse] should equal(emptyBookResponse("Foo"))
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the title" in {
    catalogueIndex indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=simple") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse] should equal(simpleBookResponse("simple"))
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the author" in {
    catalogueIndex indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=kilgore") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse] should equal(simpleBookResponse("kilgore"))
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the content" in {
    catalogueIndex indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=description") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse] should equal(simpleBookResponse("description"))
      }
    }
  }

  it should "retrieve a simple book if given a query that matches the ISBN" in {
    e2e createIndex catalogue indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=1234567890123") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse] should equal(simpleBookResponse("1234567890123"))
      }
    }
  }

  it should "retrieve an empty result set if the given query do not match any field" in {
    catalogueIndex indexAndCheck BookFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=foobar") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse] should equal(emptyBookResponse("foobar"))
      }
    }
  }

  it should "fail with a 400 (Bad Request) if the 'q' parameter is not provided" in {
    catalogueIndex andAfter { _ =>
      Get("/catalogue/search/books") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
        checkInvalidResponse("Missing search query term")
      }
    }
  }

  it should "fail with a 400 (Bad Request) if the 'q' parameter is empty" in {
    catalogueIndex andAfter { _ =>
      Get("/catalogue/search/books?q=") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
        checkInvalidResponse("Missing search query term")
      }
    }
  }

  it should "return caching directive" in {
    catalogueIndex andAfter { _ =>
      Get("/catalogue/search/books?q=whatever") ~> routes ~> check {
        val cacheHeader = header("Cache-Control").getOrElse(fail("Missing caching directive"))
        assert(cacheHeader.value.startsWith("public, max-age=60"))
        assert(header("Expires").isDefined)
      }
    }
  }
}
