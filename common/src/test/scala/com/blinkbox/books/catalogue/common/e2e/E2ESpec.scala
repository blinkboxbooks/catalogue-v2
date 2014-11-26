package com.blinkbox.books.catalogue.common.e2e

import ch.qos.logback.classic.{Level, LoggerContext}
import com.blinkbox.books.catalogue.common.SearchConfig
import com.blinkbox.books.catalogue.common.search.{Schema, EsIndexer}
import com.blinkbox.books.config.Configuration
import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.{Suite, BeforeAndAfterAll}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.Random

trait E2ESpec
    extends BeforeAndAfterAll
    with Configuration
    with E2EDsl {
  this: Suite =>

  def e2eExecutionContext: ExecutionContext
  // The random cluster name is needed to avoid race conditions between different test-suites running in parallel
  lazy val searchConfig = SearchConfig(config).copy(clusterName = Random.nextString(32))
  lazy val esServer = new EmbeddedElasticSearch(searchConfig)
  lazy val esClient = new ElasticClient(esServer.client, 2000)
  lazy val indexer = new EsIndexer(searchConfig, esClient)(e2eExecutionContext)
  lazy val catalogue = Schema(searchConfig).catalogue

  lazy val esLogLevel = Level.WARN

  lazy val e2e = using(esClient, indexer)

  def esType(`type`: String) = s"${searchConfig.indexName}/${`type`}"

  override protected def beforeAll() = {
    val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerContext.getLogger("org.elasticsearch").setLevel(esLogLevel)

    esServer.start()
  }

  override protected def afterAll() = esServer.stop()
}
