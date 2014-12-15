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
    catalogueIndex indexAndCheck (f.universe, f.everything) andAwaitFor 10.seconds
  }

  "Providing a mis-spelled query" should "result in a query suggestion" in {
    Get("/catalogue/search/books?q=uvinerse") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions should contain theSameElementsAs ("universe" :: Nil)
    }

    Get("/catalogue/search/books?q=everithing") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions should contain theSameElementsAs ("everything" :: Nil)
    }
  }

  it should "not result in a query suggestion if the word is correctly spelled" in {
    Get("/catalogue/search/books?q=universe") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions shouldBe empty
    }

    Get("/catalogue/search/books?q=everything") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions shouldBe empty
    }
  }

  it should "not result in a query suggestion if the word is totally different from any title in the index" in {
    Get("/catalogue/search/books?q=banana") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookSearchResponse].suggestions shouldBe empty
    }
  }
}
