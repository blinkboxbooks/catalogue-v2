package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.e2e.E2ESpec
import com.blinkbox.books.catalogue.common.search.Schema
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import com.sksamuel.elastic4s.{ElasticDsl => E}

class MappingsSpec extends FlatSpec with E2ESpec with Matchers with ScalaFutures {

  override implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  override implicit def patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))

  lazy val indexDef = Schema(config).catalogue

  "The mappings for the \"catalogue\" index" should "be valid for the \"book\" type" in {
    e2e createIndex indexDef andAfter { r =>
      r.isAcknowledged shouldBe true
    }
  }

  it should "ingest an empty book document and allow its retrieval" in {
    e2e createIndex(indexDef) index(Book.empty) ensure allSucceded andThen {
      esClient execute {
        E.search in "catalogue/book" query { E.matchall }
      }
    } andAfter { r =>
      r.getHits.getTotalHits should equal(1)
    }
  }
}
