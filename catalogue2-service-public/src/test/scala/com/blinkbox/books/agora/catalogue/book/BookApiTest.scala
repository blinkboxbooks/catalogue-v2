package com.blinkbox.books.agora.catalogue.book

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpecLike, Matchers}
import spray.testkit.ScalatestRouteTest
import spray.routing.HttpService
import com.blinkbox.books.test.MockitoSyrup
import org.mockito.Matchers._
import org.mockito.Mockito._
import scala.concurrent.duration._
import com.blinkbox.books.spray.v1.Version1JsonSupport
import com.blinkbox.books.spray.JsonFormats
import spray.http.StatusCodes._
import com.blinkbox.books.config.ApiConfig
import scala.concurrent.Future
import java.net.URL
import com.blinkbox.books.spray.Page
import com.blinkbox.books.spray.v1.ListPage
import com.blinkbox.books.spray.SortOrder

@RunWith(classOf[JUnitRunner])
class BookApiTest extends FlatSpecLike with ScalatestRouteTest with HttpService with Matchers with MockitoSyrup with Version1JsonSupport {
  implicit override def version1JsonFormats = JsonFormats.blinkboxFormat()
  implicit val actorRefFactory = system
  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  val apiConfig = mock[ApiConfig]
  when(apiConfig.localUrl).thenReturn(new URL("http://localhost"))
  
  val bookConfig = mock[BookConfig]
  when(bookConfig.path).thenReturn("/book")
  when(bookConfig.maxAge).thenReturn(60.seconds)
  when(bookConfig.maxResults).thenReturn(50)
  when(bookConfig.synopsisPathLink).thenReturn("synopsis")

  val service = mock[BookService]
  val api = new BookApi(apiConfig, bookConfig, service)
  val routes = api.routes
  
  val book = BookRepresentation("guid", "id", "title", "date", sampleEligible = true, List(), None)
  val defaultOrder = SortOrder("title", desc = false)
  val defaultPage = Page(0, bookConfig.maxResults)
  val expectedListPage = ListPage(1, 0, 1, List(book), None)
  val emptyListPage = ListPage(0, 0, 0, List.empty[BookRepresentation], None)

  "The service" should "return the book if it exists" in {
    when(service.getBookByIsbn(anyString)).thenReturn(Future.successful(Option(book)))
    Get("/book/isbn") ~> routes ~> check {
      verify(service).getBookByIsbn("isbn")
      status shouldEqual OK
      responseAs[BookRepresentation] shouldEqual book
    }
  }
  
  it should "return 404 if the book does not exist" in {
    when(service.getBookByIsbn(anyString)).thenReturn(Future.successful(None))
    Get("/book/cobblers") ~> routes ~> check {
      verify(service).getBookByIsbn("cobblers")
      status shouldEqual NotFound
    }
  }

  it should "return the book synopsis if it exists" in {
    val synopsis = BookSynopsis("id", "synopsis")
    when(service.getBookSynopsis(anyString)).thenReturn(Future.successful(Some(synopsis)))
    Get("/book/isbn/synopsis") ~> routes ~> check {
      verify(service).getBookSynopsis("isbn")
      status shouldEqual OK
      responseAs[BookSynopsis] shouldEqual synopsis
    }
  }

  it should "return 404 if the book synopsis does not exist" in {
    when(service.getBookSynopsis(anyString)).thenReturn(Future.successful(None))
    Get("/book/cobblers/synopsis") ~> routes ~> check {
      verify(service).getBookSynopsis("cobblers")
      status shouldEqual NotFound
    }
  }
  
  it should "return bulk books" in {
    val isbns = List("1", "2")
    val expected = ListPage(2,0,2,List(book, book),None)
    when(service.getBooks(eql(isbns), any[Page])).thenReturn(Future.successful(expected))
    Get("/book/?id=1&id=2") ~> routes ~> check {
      verify(service).getBooks(isbns, Page(0, bookConfig.maxResults))
      status shouldEqual OK
      responseAs[ListPage[BookRepresentation]] shouldEqual expected
    }
  }
  
  it should "return paginated bulk books" in {
    val isbns = List("1", "2", "3")
    val expected = ListPage(3,1,1,List(book, book),None)
    when(service.getBooks(isbns, Page(1, 1))).thenReturn(Future.successful(expected))
    Get("/book/?id=1&id=2&id=3&offset=1&count=1") ~> routes ~> check {
      verify(service).getBooks(isbns, Page(1, 1))
      status shouldEqual OK
      responseAs[ListPage[BookRepresentation]] shouldEqual expected
    }
  }
  
  it should "return empty results for unknown books" in {
    when(service.getBooks(any[List[String]], any[Page])).thenReturn(Future.successful(emptyListPage))
    Get("/book/?id=999") ~> routes ~> check {
      status shouldEqual OK
      responseAs[ListPage[BookRepresentation]] shouldEqual emptyListPage
    }
  }
  
  it should "return books by the given contributor" in {
    when(service.getBooksByContributor("42", None, None, defaultPage, defaultOrder)).thenReturn(Future.successful(expectedListPage))
    Get("/book/?contributor=42") ~> routes ~> check {
      status shouldEqual OK
      responseAs[ListPage[BookRepresentation]] shouldEqual expectedListPage
    }
  }
  
  it should "return empty results for an unknown contributor" in {
    when(service.getBooksByContributor("999", None, None, defaultPage, defaultOrder)).thenReturn(Future.successful(emptyListPage))
    Get("/book/?contributor=999") ~> routes ~> check {
      status shouldEqual OK
      responseAs[ListPage[BookRepresentation]] shouldEqual emptyListPage
    }
  }
  
  it should "return books by a given contributor with a specified publication date-range" in {
    val dateParam = "2014-01-01"
    val date = BookService.dateTimeFormat.parseDateTime(dateParam)
    when(service.getBooksByContributor("42", Some(date), Some(date), Page(0, bookConfig.maxResults), SortOrder("title", desc = false))).thenReturn(Future.successful(expectedListPage))
    Get(s"/book/?contributor=42&minPublicationDate=$dateParam&maxPublicationDate=$dateParam") ~> routes ~> check {
      status shouldEqual OK
      responseAs[ListPage[BookRepresentation]] shouldEqual expectedListPage
    }
  }
  
  it should "fail if the end-date is before the start-date" in {
    Get(s"/book/?contributor=42&minPublicationDate=2014-01-01&maxPublicationDate=2013-01-01") ~> routes ~> check {
      status shouldEqual BadRequest
    }
  }
  
  it should "Return books by a given contributor with a specific sort order" in {
    when(service.getBooksByContributor("42", None, None, Page(0, bookConfig.maxResults), SortOrder("author", desc = true))).thenReturn(Future.successful(expectedListPage))
    Get(s"/book/?contributor=42&order=author&desc=true") ~> routes ~> check {
      status shouldEqual OK
      responseAs[ListPage[BookRepresentation]] shouldEqual expectedListPage
    }
  }

  it should "fail if given an invalid sort order" in {
    Get(s"/book/?contributor=42&order=cobblers") ~> routes ~> check {
      status shouldEqual BadRequest
    }
  }
}
