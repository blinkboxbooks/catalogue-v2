package com.blinkbox.books.catalogue.browser

import java.net.URL

import com.blinkbox.books.catalogue.browser.v1.{SearchApi, EsV1SearchService}
import com.blinkbox.books.catalogue.common.e2e.E2ESpec
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.json.DefaultFormats
import org.json4s.Formats
import org.scalatest.Suite
import org.scalatest.time.{Millis, Span}
import spray.httpx.Json4sJacksonSupport
import spray.testkit.ScalatestRouteTest
import scala.concurrent.duration._

trait ApiSpecBase extends E2ESpec with ScalatestRouteTest with Json4sJacksonSupport {
  this: Suite =>

  override implicit def patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))
  override def e2eExecutionContext = executor
  override implicit def json4sJacksonFormats: Formats = DefaultFormats

  implicit val routeTestTimeout = RouteTestTimeout(3.seconds)

  lazy val searchService = new EsV1SearchService(searchConfig, esClient)
  lazy val apiUrl = new URL("http://localhost:9595")
  lazy val apiConfig = ApiConfig(apiUrl, apiUrl, 2.seconds)
  lazy val apiService = new SearchApi(apiConfig, searchService)
  lazy val routes = apiService.routes
}
