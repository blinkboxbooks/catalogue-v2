package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.BookFixtures
import com.blinkbox.books.catalogue.searchv1.V1SearchService.BookSearchResponse
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes
import scala.concurrent.duration._

class EmptyIndexSpecs extends FlatSpec with Matchers with ApiSpecBase {

  import com.blinkbox.books.catalogue.searchv1.ResponseFixtures._

  override def beforeAll() {
    super.beforeAll()
    catalogueIndex andAwaitFor(15.seconds)
  }

  "The search API" should "fail with a 400 (Bad Request) if the 'q' parameter is not provided" in {
    Get("/catalogue/search/books") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse("Missing search query term")
    }
  }

  it should "fail with a 400 (Bad Request) if the 'q' parameter is empty" in {
    Get("/catalogue/search/books?q=") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse("Missing search query term")
    }
  }

  it should "retrieve empty result-sets from an empty index" in {
    Get("/catalogue/search/books?q=Foo") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse] should equal(emptyBookResponse("Foo"))
    }
  }

  it should "return caching directive" in {
    Get("/catalogue/search/books?q=whatever") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      val cacheHeader = header("Cache-Control").getOrElse(fail("Missing caching directive"))
      assert(cacheHeader.value.startsWith("public, max-age=60"))
      assert(header("Expires").isDefined)
    }
  }
}
