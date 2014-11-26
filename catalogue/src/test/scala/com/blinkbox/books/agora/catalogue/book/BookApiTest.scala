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
import com.blinkbox.books.agora.catalogue.app.AppConfig
import com.blinkbox.books.config.ApiConfig
import scala.concurrent.Future
import java.net.URL

@RunWith(classOf[JUnitRunner])
class WebAppTest extends FlatSpecLike with ScalatestRouteTest with HttpService with Matchers with MockitoSyrup with Version1JsonSupport {
  implicit override def version1JsonFormats = JsonFormats.blinkboxFormat()
  implicit val actorRefFactory = system
  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  val apiConfig = mock[ApiConfig]
  when(apiConfig.localUrl).thenReturn(new URL("http://localhost"))
  
  val bookConfig = mock[BookConfig]
  when(bookConfig.path).thenReturn("")
  when(bookConfig.maxAge).thenReturn(60.seconds)

  val service = mock[BookService]
  val api = new BookApi(apiConfig, bookConfig, service)
  val routes = api.routes

  it should "return the book if it exists" in {
    val book = BookRepresentation("guid", "id", "title", "date", true, List(), None)
    when(service.getBookByIsbn(anyString)).thenReturn(Future.successful(Option(book)))
    Get("/isbn") ~> routes ~> check {
      verify(service).getBookByIsbn("isbn")
      status shouldEqual OK
      responseAs[BookRepresentation] shouldEqual book
    }
  }
  
  it should "return 404 if the book does not exist" in {
    when(service.getBookByIsbn(anyString)).thenReturn(Future.successful(None))
    Get("/cobblers") ~> routes ~> check {
      verify(service).getBookByIsbn("cobblers")
      status shouldEqual NotFound
    }
  }

  it should "return the book synopsis if it exists" in {
    val synopsis = BookSynopsis("id", "synopsis")
    when(service.getBookSynopsis(anyString)).thenReturn(Future.successful(Some(synopsis)))
    Get("/isbn/synopsis") ~> routes ~> check {
      verify(service).getBookSynopsis("isbn")
      status shouldEqual OK
      responseAs[BookSynopsis] shouldEqual synopsis
    }
  }

  it should "return 404 if the book synopsis does not exist" in {
    when(service.getBookSynopsis(anyString)).thenReturn(Future.successful(None))
    Get("/cobblers/synopsis") ~> routes ~> check {
      verify(service).getBookSynopsis("cobblers")
      status shouldEqual NotFound
    }
  }
}
