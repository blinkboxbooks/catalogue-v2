package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.searchv1.V1SearchService.{Book => BookResponse, BookSearchResponse}
import com.blinkbox.books.catalogue.common.Events.{Book => BookMessage}
import com.blinkbox.books.catalogue.common.BookFixtures
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class TitleSearchSpecs extends FlatSpec with Matchers with ApiSpecBase {

  val f = BookFixtures

  private def queryAndCheck[T](q: String)(f: => T) = Get(s"/catalogue/search/books?q=$q") ~> routes ~> check(f)

  private def toBookResponse(q: String, total: Int, books: BookMessage*): BookSearchResponse = {
    val booksList = if (books.size == 0) None else Some(books.map { b =>
      BookResponse(b.isbn, b.title, b.contributors.filter(_.role.toLowerCase == "author").map(_.displayName))
    }.toList)

    BookSearchResponse(q, booksList, total)
  }

  "Matching a document" should "ignore stop-words in the document title" in {
    catalogueIndex indexAndCheck(f.theUniverse, f.universe, f.theUniverseAndOtherThings) andAfter { _ =>
      queryAndCheck("universe") {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse].numberOfResults should equal(3)
      }
    }
  }

  it should "ignore stop-words in the provided query" in {
    catalogueIndex indexAndCheck(f.theUniverse, f.universe, f.theUniverseAndOtherThings) andAfter { _ =>
      queryAndCheck("a%20the%20for%20universe") {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse].numberOfResults should equal(3)
      }
    }
  }

  it should "rank documents that perfectly match the title on top of documents that match but include also stop-words" in {
    catalogueIndex indexAndCheck(f.theUniverseAndOtherThings, f.theUniverse, f.universe) andAfter { _ =>
      queryAndCheck("universe") {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[BookSearchResponse]

        respBooks.numberOfResults should equal(3)
        respBooks should equal(toBookResponse("universe", 3, f.universe, f.theUniverse, f.theUniverseAndOtherThings))
      }
    }
  }

  it should "rank documents that perfectly match the title including stop-words in the query on top of documents that match but do not have the provided stop-words in the title" in {
    catalogueIndex indexAndCheck(f.theUniverseAndOtherThings, f.theUniverse, f.universe) andAfter { _ =>
      queryAndCheck("the%20universe") {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[BookSearchResponse]

        respBooks.numberOfResults should equal(3)
        respBooks should equal(toBookResponse("the universe", 3, f.theUniverse, f.theUniverseAndOtherThings, f.universe))
      }
    }
  }

  it should "rank documents that match on the title on top of documents that match on the content" in {
    catalogueIndex indexAndCheck(f.theUniverseAndOtherThings, f.everything, f.theUniverse, f.universe) andAfter { _ =>
      queryAndCheck("universe") {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[BookSearchResponse]

        respBooks.numberOfResults should equal(4)
        respBooks should equal(toBookResponse("universe", 4, f.universe, f.theUniverse, f.theUniverseAndOtherThings, f.everything))
      }
    }
  }

  it should "rank a document that matches perfectly on the title above a document that matches both title (partially) and content" in {
    catalogueIndex indexAndCheck(f.universeAndOtherThingsWithDescription, f.theUniverse, f.universe) andAfter { _ =>
      queryAndCheck("universe") {
        status should equal(StatusCodes.OK)

        val respBooks = responseAs[BookSearchResponse]

        respBooks.numberOfResults should equal(3)
        respBooks should equal(toBookResponse("universe", 3, f.universe, f.theUniverse, f.universeAndOtherThingsWithDescription))
      }
    }
  }

  it should "work correctly with title permutations (see CAT-77)" in {
    catalogueIndex indexAndCheck(f.titlePermutationsBook) andAfter { _ =>
      queryAndCheck("apple%20banana") {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse].numberOfResults should equal(1)
      }

      queryAndCheck("apple%20banana%20pear") {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse].numberOfResults should equal(1)
      }

      queryAndCheck("apple%20pear") {
        status should equal(StatusCodes.OK)
        responseAs[BookSearchResponse].numberOfResults should equal(1)
      }
    }
  }
}
