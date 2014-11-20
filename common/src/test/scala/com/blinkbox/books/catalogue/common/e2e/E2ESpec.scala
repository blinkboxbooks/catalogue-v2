package com.blinkbox.books.catalogue.common.e2e

import ch.qos.logback.classic.{Level, LoggerContext}
import com.blinkbox.books.catalogue.common.search.EsIndexer
import com.blinkbox.books.config.Configuration
import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.{Suite, BeforeAndAfterAll}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

trait E2ESpec
    extends BeforeAndAfterAll
    with Configuration
    with E2EDsl {
  this: Suite =>

  implicit def executionContext: ExecutionContext
  lazy val esServer = new EmbeddedElasticSearch
  lazy val esClient = new ElasticClient(esServer.client, 2000)
  lazy val indexer = new EsIndexer(config, esClient)
  lazy val esLogLevel = Level.WARN

  lazy val e2e = using(esClient, indexer)

  override protected def beforeAll() = {
    val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerContext.getLogger("org.elasticsearch").setLevel(esLogLevel)

    esServer.start()
  }

  override protected def afterAll() = esServer.stop()
}
