package com.blinkbox.books.catalogue.searchv1

import java.net.URL

import com.blinkbox.books.catalogue.common.e2e.E2ESpec
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.spray.v1.Version1JsonSupport
import org.scalatest.Suite
import org.scalatest.time.{Millis, Span}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._

trait ApiSpecBase extends E2ESpec with ScalatestRouteTest with Version1JsonSupport {
  this: Suite =>

  override implicit def patienceConfig = PatienceConfig(timeout = Span(5000, Millis), interval = Span(100, Millis))
  override def e2eExecutionContext = executor

  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  lazy val searchService = new EsV1SearchService(searchConfig, esClient)
  lazy val apiUrl = new URL("http://localhost:9595")
  lazy val apiConfig = ApiConfig(apiUrl, apiUrl, 2.seconds)
  lazy val apiService = new SearchApi(apiConfig, searchService)
  lazy val routes = apiService.routes

  def catalogueIndex = e2e createIndex catalogue
}
