package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.searchv1.V1SearchService.BookSearchResponse
import com.blinkbox.books.catalogue.common.BookFixtures
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.duration._
import spray.http.StatusCodes

class SpellcheckSpecs extends FlatSpec with Matchers with ApiSpecBase {
  val f = BookFixtures

  override def beforeAll(): Unit = {
    super.beforeAll()
    catalogueIndex indexAndCheck (f.universe, f.everything, f.simpleBook) andAwaitFor 10.seconds
  }

  def suggestions(resp: BookSearchResponse): Seq[String] = resp.suggestions.getOrElse(Nil)

  "Providing a query to the search service" should "result in a query suggestion if it only contains a mis-spelled word" in {
    Get("/catalogue/search/books?q=uvinerse") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      suggestions(responseAs[BookSearchResponse]) should contain theSameElementsAs ("universe" :: Nil)
    }

    Get("/catalogue/search/books?q=everithing") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      suggestions(responseAs[BookSearchResponse]) should contain theSameElementsAs ("everything" :: Nil)
    }
  }

  it should "result in a query suggestion if it contains a mis-spelled word and a non-misspelled word" in {
    Get("/catalogue/search/books?q=uvinerse%20everything") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      suggestions(responseAs[BookSearchResponse]) should contain theSameElementsAs ("universe everything" :: Nil)
    }
  }

  it should "result in a query suggestion if it contains multiple mis-spelled words" in {
    Get("/catalogue/search/books?q=uvinerse%20eveyrthing%20simple") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      suggestions(responseAs[BookSearchResponse]) should contain theSameElementsAs ("universe everything simple" :: Nil)
    }
  }

  it should "not result in a query suggestion if the word is correctly spelled" in {
    Get("/catalogue/search/books?q=universe") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions should equal(None)
    }

    Get("/catalogue/search/books?q=everything") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions should equal(None)
    }
  }

  it should "not result in a query suggestion if the word is totally different from any title in the index" in {
    Get("/catalogue/search/books?q=banana") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions should equal(None)
    }
  }
}
