package com.blinkbox.books.catalogue.common.e2e

import ch.qos.logback.classic.{Level, LoggerContext}
import com.blinkbox.books.catalogue.common.ElasticsearchConfig
import com.blinkbox.books.catalogue.common.search.{HttpEsIndexer, Schema}
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.elasticsearch.client.SprayElasticClient
import org.scalatest.time.Millis
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory

trait HttpEsSpec
    extends BeforeAndAfterAll
    with Configuration
    with E2EDsl {
  this: Suite with spray.testkit.RouteTest =>

  // Lower ES log level in order to suppress all debugging exceptions
  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    .getLogger("org.elasticsearch").setLevel(Level.WARN)

  override implicit def patienceConfig = PatienceConfig(timeout = Span(30000, Millis), interval = Span(250, Millis))

  lazy val esHttpPort = 12000 + (Thread.currentThread.getId % 100).toInt
  lazy val searchConfig = ElasticsearchConfig(config).copy(httpPort = esHttpPort)
  lazy val esServer = new EmbeddedElasticSearch(searchConfig)
  lazy val esClient = new SprayElasticClient("localhost", esHttpPort)
  lazy val indexer = new HttpEsIndexer(searchConfig, esClient)
  lazy val catalogue = Schema(searchConfig).catalogue

  lazy val e2e = using(esClient, indexer)

  def esType(`type`: String) = s"${searchConfig.indexName}/${`type`}"

  override protected def beforeAll() = {
    esServer.start()
  }

  override protected def afterAll() = esServer.stop()
}
