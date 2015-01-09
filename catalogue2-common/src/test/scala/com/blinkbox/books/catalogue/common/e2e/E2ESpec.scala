package com.blinkbox.books.catalogue.common.e2e

import akka.actor.ActorSystem
import ch.qos.logback.classic.{Level, LoggerContext}
import com.blinkbox.books.catalogue.common.{ElasticsearchConfig, Json}
import com.blinkbox.books.catalogue.common.search.{EsIndexer, HttpEsIndexer, Schema}
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.elasticsearch.client.SprayElasticClient
import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.time.Millis
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.Random

@deprecated("Existing tests should be migrated to HttpEsSpec so this could be removed", "0.1.66")
trait E2ESpec
    extends BeforeAndAfterAll
    with Configuration
    with E2EDsl {
  this: Suite =>

  val alphabet = "abcdefghijklmnopqrstuvwxyz01234567890"
  val randomName = (0 to 16).foldLeft("")((str, _) => str + alphabet.charAt(Random.nextInt(alphabet.length)))

  // Lower ES log level in order to suppress all debugging exceptions
  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    .getLogger("org.elasticsearch").setLevel(Level.WARN)

  def e2eExecutionContext: ExecutionContext
  // The random cluster name is needed to avoid race conditions between different test-suites running in parallel
  lazy val searchConfig = ElasticsearchConfig(config).copy(clusterName = randomName)
  lazy val esServer = new EmbeddedElasticSearch(searchConfig)
  lazy val esClient = new ElasticClient(esServer.client, 2000)
  lazy val indexer = new EsIndexer(searchConfig, esClient)(e2eExecutionContext)
  lazy val catalogue = Schema(searchConfig).catalogue

  lazy val e2e = using(esClient, indexer)(e2eExecutionContext)

  def esType(`type`: String) = s"${searchConfig.indexName}/${`type`}"

  override protected def beforeAll() = {
    esServer.start()
  }

  override protected def afterAll() = esServer.stop()
}

trait HttpEsSpec
    extends BeforeAndAfterAll
    with Configuration
    with E2EDsl {
  this: Suite with spray.testkit.RouteTest =>

  // Lower ES log level in order to suppress all debugging exceptions
  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    .getLogger("org.elasticsearch").setLevel(Level.WARN)

  override implicit def patienceConfig = PatienceConfig(timeout = Span(30000, Millis), interval = Span(250, Millis))

  import Json.json4sUnmarshaller

  lazy val esPort = 12000 + (Thread.currentThread.getId % 100).toInt

  lazy val searchConfig = ElasticsearchConfig(config).copy(httpPort = esPort)
  lazy val esServer = new EmbeddedElasticSearch(searchConfig)
  lazy val esClient = new SprayElasticClient("localhost", esPort)
  lazy val indexer = new HttpEsIndexer(searchConfig, esClient)
  lazy val catalogue = Schema(searchConfig).catalogue

  lazy val e2e = using(esClient, indexer)

  def esType(`type`: String) = s"${searchConfig.indexName}/${`type`}"

  override protected def beforeAll() = {
    esServer.start()
  }

  override protected def afterAll() = esServer.stop()
}
