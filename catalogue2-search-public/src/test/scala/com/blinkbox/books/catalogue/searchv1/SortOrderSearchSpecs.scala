package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.searchv1.V1SearchService.{Book => BookResponse, BookSearchResponse}
import com.blinkbox.books.catalogue.common.BookFixtures
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.Await
import scala.concurrent.duration._

class SortOrderSearchSpecs extends FlatSpec with Matchers with ApiSpecBase with BeforeAndAfterAll {
  val books = {
    import BookFixtures._
    List(universe, everything)
  }
  
  val expected = books.map { b => BookResponse(b.isbn, b.title, b.contributors.filter(_.role.toLowerCase == "author").map(_.displayName)) }

  override def beforeAll(): Unit = {
    def populateIndex = catalogueIndex index(books.toSeq: _*)
    super.beforeAll()
    Await.ready(populateIndex.state, 10.seconds)
  }

  private def query[T](order: String, desc: Boolean)(f: => T) = Get(s"/catalogue/search/books?q=universe&order=$order&desc=$desc") ~> routes ~> check(f)
  
  "Search" should "order books by relevance" in {
    query("relevance", desc = true) {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].books should equal(Some(expected))
    }
  }
  
  it should "order books by relevance ascending" in {
    query("relevance", desc = false) {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].books should equal(Some(expected.reverse))
    }
  }
  
  it should "return bad request for an invalid sort order" in {
    query("cobblers", desc = true) {
      status should equal(StatusCodes.BadRequest)
    }
  }
}
