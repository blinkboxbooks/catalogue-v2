package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.BookFixtures
import com.blinkbox.books.catalogue.searchv1.V1SearchService.{BookSimilarResponse, BookSuggestionResponse, BookSearchResponse}
import com.sksamuel.elastic4s.{CompletionSuggestionDefinition, GetDefinition}
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class PaginationSpecs extends FlatSpec with Matchers with ApiSpecBase {

  def populateIndex(howManyBooks: Int) = catalogueIndex indexAndCheck(BookFixtures.dummyBooks(howManyBooks).toSeq: _*)

  "Search pagination" should "provide 50 books per page if no parameter is specified" in {
    populateIndex(60) andAfter { _ =>
      Get("/catalogue/search/books?q=dummy") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val resp = responseAs[BookSearchResponse]

        resp.numberOfResults should equal(60)
        resp.books.size should equal(50)
      }
    }
  }

  it should "allow access to the second page of results if only the offset parameter is provided" in {
    populateIndex(60) andAfter { _ =>
      Get("/catalogue/search/books?q=dummy&offset=50") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val resp = responseAs[BookSearchResponse]

        resp.numberOfResults should equal(60)
        resp.books.size should equal(10)
      }
    }
  }

  it should "allow selecting the page size" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/books?q=dummy&count=5") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val resp = responseAs[BookSearchResponse]

        resp.numberOfResults should equal(10)
        resp.books.size should equal(5)
      }
    }
  }

  it should "allow selecting the page size and the offset at the same time" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/books?q=dummy&count=5&offset=7") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val resp = responseAs[BookSearchResponse]

        resp.numberOfResults should equal(10)
        resp.books.size should equal(3)
      }
    }
  }

  it should "fail with a 400 (Bad Request) if a negative count is provided" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/books?q=dummy&count=-1") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
      }
    }
  }

  it should "fail with a 400 (Bad Request) if a 0 count is provided" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/books?q=dummy&count=0") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
      }
    }
  }

  it should "fail with a 400 (Bad Request) if a negative offset is provided" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/books?q=dummy&offset=-1") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
      }
    }
  }

  "Suggestions pagination" should "return 10 results if no limit is provided" in {
    populateIndex(15) andAfter { _ =>
      Get("/catalogue/search/suggestions?q=dum") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSuggestionResponse].items.size should equal(10)
      }
    }
  }

  it should "return a specified number of result if limit is provided" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/suggestions?q=dummy&limit=5") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[BookSuggestionResponse].items.size should equal(5)
      }
    }
  }

  it should "return a 400 (Bad Request) if a 0 limit is provided" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/suggestions?q=dummy&limit=0") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
      }
    }
  }

  it should "return a 400 (Bad Request) if a negative limit is provided" in {
    populateIndex(10) andAfter { _ =>
      Get("/catalogue/search/suggestions?q=dummy&limit=-1") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
      }
    }
  }
}
