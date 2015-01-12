package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.e2e.HttpEsSpec
import com.blinkbox.books.elasticsearch.client.ElasticClientApi
import com.sksamuel.elastic4s.{ElasticDsl => E}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import spray.testkit.ScalatestRouteTest

class MappingsSpec extends FlatSpec with HttpEsSpec with Matchers with ScalaFutures with ScalatestRouteTest{
  import ElasticClientApi._
  import Json._

  override implicit def patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))

  "The mappings for the \"catalogue\" index" should "be valid for the \"book\" type" in {
    e2e createIndex catalogue andAfter { r =>
      r.acknowledged shouldBe true
    }
  }

  it should "ingest an empty book document and allow its retrieval" in {
    e2e createIndex catalogue indexAndCheck BookFixtures.simpleBook andThen {
      esClient execute {
        E.search in esType("book") query { E.matchall }
      }
    } andAfter { r =>
      r.hits.total should equal(1)
    }
  }
}
