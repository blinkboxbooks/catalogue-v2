package com.blinkbox.books.catalogue.browser

import com.blinkbox.books.catalogue.browser.v1.V1SearchService.{Book => BookResponse}
import com.blinkbox.books.catalogue.common.Events.{Book => BookMessage}
import com.blinkbox.books.catalogue.common.BookFixtures
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class TitleSearchSpecs extends FlatSpec with Matchers with ApiSpecBase {

  def catalogueIndex = e2e createIndex catalogue
  val f = BookFixtures

  private def toBookResponse(books: BookMessage*): List[BookResponse] = books.map { b =>
    BookResponse(b.isbn, b.title, b.contributors.filter(_.role.toLowerCase == "author").map(_.displayName))
  }.toList

  "Matching a document" should "ignore stop-words in the document title" in {
    catalogueIndex indexAndCheck(f.theUniverse, f.universe, f.theUniverseAndOtherThings) andAfter { _ =>
      Get("/catalogue/search/books?q=universe") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[BookResponse]].size should equal(3)
      }
    }
  }

  it should "ignore stop-words in the provided query" in {
    catalogueIndex indexAndCheck(f.theUniverse, f.universe, f.theUniverseAndOtherThings) andAfter { _ =>
      Get("/catalogue/search/books?q=a%20the%20for%20universe") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[BookResponse]].size should equal(3)
      }
    }
  }

  it should "rank documents that perfectly match the title on top of documents that match but include also stop-words" in {
    catalogueIndex indexAndCheck(f.theUniverseAndOtherThings, f.theUniverse, f.universe) andAfter { _ =>
      Get("/catalogue/search/books?q=universe") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[List[BookResponse]]

        respBooks.size should equal(3)
        respBooks should equal(toBookResponse(f.universe, f.theUniverse, f.theUniverseAndOtherThings))
      }
    }
  }

  it should "rank documents that perfectly match the title including stop-words in the query on top of documents that match but do not have the provided stop-words in the title" in {
    catalogueIndex indexAndCheck(f.theUniverseAndOtherThings, f.theUniverse, f.universe) andAfter { _ =>
      Get("/catalogue/search/books?q=the%20universe") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[List[BookResponse]]

        respBooks.size should equal(3)
        respBooks should equal(toBookResponse(f.theUniverse, f.theUniverseAndOtherThings, f.universe))
      }
    }
  }

  it should "rank documents that match on the title on top of documents that match on the content" in {
    catalogueIndex indexAndCheck(f.theUniverseAndOtherThings, f.everything, f.theUniverse, f.universe) andAfter { _ =>
      Get("/catalogue/search/books?q=universe") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[List[BookResponse]]

        respBooks.size should equal(4)
        respBooks should equal(toBookResponse(f.universe, f.theUniverse, f.theUniverseAndOtherThings, f.everything))
      }
    }
  }

  it should "rank a document that matches perfectly on the title above a document that matches both title (partially) and content" in {
    catalogueIndex indexAndCheck(f.universeAndOtherThingsWithDescription, f.theUniverse, f.universe) andAfter { _ =>
      Get("/catalogue/search/books?q=universe") ~> routes ~> check {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[List[BookResponse]]

        respBooks.size should equal(3)
        respBooks should equal(toBookResponse(f.universe, f.theUniverse, f.universeAndOtherThingsWithDescription))
      }
    }
  }
}
