package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.searchv1.V1SearchService.{Book, BookSearchResponse, BookSimilarResponse, BookCompletionResponse}
import com.blinkbox.books.spray.v1.Link
import org.json4s.Extraction
import org.json4s.JsonAST._
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

import scala.concurrent.duration._

class PaginationSpecs extends FlatSpec with Matchers with ApiSpecBase {

  override def beforeAll(): Unit = {
    super.beforeAll()
    populateDummyIndex(100) andAwaitFor (10.seconds)
  }

  def checkLinks(response: JObject, hasPrev: Boolean = false, hasNext: Boolean = false): Unit = {
    val links = responseAs[JObject] \ "links"
    links shouldBe a [JArray]

    val deserialisedLinks = Extraction.extract[Seq[Link]](links)
    deserialisedLinks.find(_.rel == "this") shouldBe defined
    if (hasNext) deserialisedLinks.find(_.rel == "next") shouldBe defined
    if (hasPrev) deserialisedLinks.find(_.rel == "prev") shouldBe defined
  }

  implicit class ReturnedBooksSupport[T <: { def books: Option[Seq[Book]] }](resp: T) {
    import scala.language.reflectiveCalls
    val returnedBooks = resp.books.fold(0)(_.size)
  }

  "Search pagination" should "provide 50 books per page if no parameter is specified" in {
    Get("/catalogue/search/books?q=dummy") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSearchResponse]

      resp.numberOfResults should equal(100)
      resp.returnedBooks should equal(50)

      checkLinks(responseAs[JObject], hasNext = true)
    }
  }

  it should "allow access to the second page of results if only the offset parameter is provided" in {
    Get("/catalogue/search/books?q=dummy&offset=90") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSearchResponse]

      resp.numberOfResults should equal(100)
      resp.returnedBooks should equal(10)

      checkLinks(responseAs[JObject], hasPrev = true)
    }
  }

  it should "allow selecting the page size" in {
    Get("/catalogue/search/books?q=dummy&count=5") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSearchResponse]

      resp.numberOfResults should equal(100)
      resp.returnedBooks should equal(5)

      checkLinks(responseAs[JObject], hasNext = true)
    }
  }

  it should "allow selecting the page size and the offset at the same time" in {
    Get("/catalogue/search/books?q=dummy&count=10&offset=95") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSearchResponse]

      resp.numberOfResults should equal(100)
      resp.returnedBooks should equal(5)

      checkLinks(responseAs[JObject], hasPrev = true)
    }
  }

  it should "fail with a 400 (Bad Request) if a negative count is provided" in {
    Get("/catalogue/search/books?q=dummy&count=-1") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  it should "fail with a 400 (Bad Request) if a 0 count is provided" in {
    Get("/catalogue/search/books?q=dummy&count=0") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  it should "fail with a 400 (Bad Request) if a negative offset is provided" in {
    Get("/catalogue/search/books?q=dummy&offset=-1") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  "Suggestions pagination" should "return 10 results if no limit is provided" in {
    Get("/catalogue/search/suggestions?q=dum") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookCompletionResponse].items.size should equal(10)
    }
  }

  it should "return a specified number of result if limit is provided" in {
    Get("/catalogue/search/suggestions?q=dummy&limit=5") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      responseAs[BookCompletionResponse].items.size should equal(5)
    }
  }

  it should "return a 400 (Bad Request) if a 0 limit is provided" in {
    Get("/catalogue/search/suggestions?q=dummy&limit=0") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  it should "return a 400 (Bad Request) if a negative limit is provided" in {
    Get("/catalogue/search/suggestions?q=dummy&limit=-1") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  "More Like This pagination" should "return 10 elements if no count is provided" in {
    Get("/catalogue/search/books/0000000000001/similar") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSimilarResponse]
      resp.numberOfResults should equal(99)
      resp.returnedBooks should equal(10)

      checkLinks(responseAs[JObject], hasNext = true)
    }
  }


  it should "return the specified number of elements if a count is provided" in {
    Get("/catalogue/search/books/0000000000001/similar?count=5") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSimilarResponse]
      resp.numberOfResults should equal(99)
      responseAs[BookSimilarResponse].returnedBooks should equal(5)

      checkLinks(responseAs[JObject], hasNext = true)
    }
  }

  it should "observe the offset parameter when provided" in {
    Get("/catalogue/search/books/0000000000001/similar?offset=94") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSimilarResponse]
      resp.numberOfResults should equal(99)
      resp.returnedBooks should equal(5)

      checkLinks(responseAs[JObject], hasPrev = true)
    }
  }

  it should "observe offset and count parameters if both are provided" in {
    Get("/catalogue/search/books/0000000000001/similar?offset=79&count=25") ~> routes ~> check {
      status should equal(StatusCodes.OK)

      val resp = responseAs[BookSimilarResponse]
      resp.numberOfResults should equal(99)
      resp.returnedBooks should equal(20)

      checkLinks(responseAs[JObject], hasPrev = true)
    }
  }

  it should "fail with a 400 (Bad Request) if a negative count is provided" in {
    Get("/catalogue/search/books/0000000000001/similar?count=-1") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  it should "fail with a 400 (Bad Request) if a 0 count is provided" in {
    Get("/catalogue/search/books/0000000000001/similar?count=0") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  it should "fail with a 400 (Bad Request) if a negative offset is provided" in {
    Get("/catalogue/search/books/0000000000001/similar?offset=-1") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }
}
