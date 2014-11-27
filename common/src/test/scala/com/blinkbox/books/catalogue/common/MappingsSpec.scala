package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common.e2e.E2ESpec
import com.sksamuel.elastic4s.{ElasticDsl => E}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}

class MappingsSpec extends FlatSpec with E2ESpec with Matchers with ScalaFutures {

  override implicit val e2eExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  override implicit def patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))

  "The mappings for the \"catalogue\" index" should "be valid for the \"book\" type" in {
    e2e createIndex catalogue andAfter { r =>
      r.isAcknowledged shouldBe true
    }
  }

  it should "ingest an empty book document and allow its retrieval" in {
    e2e createIndex catalogue indexAndCheck Book.empty andThen {
      esClient execute {
        E.search in esType("book") query { E.matchall }
      }
    } andAfter { r =>
      r.getHits.getTotalHits should equal(1)
    }
  }
}
