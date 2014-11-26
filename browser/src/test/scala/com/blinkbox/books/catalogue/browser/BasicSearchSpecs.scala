package com.blinkbox.books.catalogue.browser

import java.net.URL
import java.util.Map.Entry

import com.blinkbox.books.catalogue.browser.v1.V1SearchService.Book
import com.blinkbox.books.catalogue.browser.v1.{EsV1SearchService, SearchApi}
import com.blinkbox.books.catalogue.common.MessageFixtures
import com.blinkbox.books.catalogue.common.e2e.E2ESpec
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.json.DefaultFormats
import com.typesafe.config.{Config, ConfigValue}
import org.json4s.Formats
import org.scalatest.time.{Millis, Span}
import org.scalatest.{Matchers, FlatSpec}
import spray.http.StatusCodes
import spray.httpx.Json4sJacksonSupport
import spray.testkit.ScalatestRouteTest
import scala.concurrent.duration._

class BasicSearchSpecs extends FlatSpec with E2ESpec with Matchers with ScalatestRouteTest with Json4sJacksonSupport {

  override implicit def patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))
  override def e2eExecutionContext = executor
  override implicit def json4sJacksonFormats: Formats = DefaultFormats

  implicit val routeTestTimeout = RouteTestTimeout(3.seconds)

  lazy val searchService = new EsV1SearchService(searchConfig, esClient)
  lazy val apiUrl = new URL("http://localhost:9595")
  lazy val apiConfig = ApiConfig(apiUrl, apiUrl, 2.seconds)
  lazy val apiService = new SearchApi(apiConfig, searchService)
  lazy val routes = apiService.routes

  val simpleBookResponse = Book("1234567890123", "A simple book", "Foo C. Bar" :: Nil) :: Nil

  "The search API" should "retrieve empty result-sets from an empty index" in {
    e2e createIndex catalogue andAfter { _ =>
      Get("/catalogue/search/books?q=Foo") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(Nil)
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the title" in {
    e2e createIndex catalogue indexAndCheck MessageFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=simple") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(simpleBookResponse)
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the author" in {
    e2e createIndex catalogue indexAndCheck MessageFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=foo") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(simpleBookResponse)
      }
    }
  }

  it should "retrieve a simple book if given a query that match in the content" in {
    e2e createIndex catalogue indexAndCheck MessageFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=description") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(simpleBookResponse)
      }
    }
  }

  it should "retrieve an empty result set if the given query do not match any field" in {
    e2e createIndex catalogue indexAndCheck MessageFixtures.simpleBook andAfter { _ =>
      Get("/catalogue/search/books?q=foobar") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        responseAs[List[Book]] should equal(Nil)
      }
    }
  }

  it should "fail with a 400 (Bad Request) if the 'q' parameter is not provided" in {
    e2e createIndex catalogue andAfter { _ =>
      Get("/catalogue/search/books") ~> routes ~> check {
        status should equal(StatusCodes.BadRequest)
      }
    }
  }
}
