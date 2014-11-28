package com.blinkbox.books.agora.catalogue.book

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import org.mockito.Matchers._
import org.mockito.Mockito._
import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.time.{Millis, Span}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common._
import com.blinkbox.books.catalogue.common.e2e.E2ESpec
import com.blinkbox.books.catalogue.common.BookFixtures.simpleBook
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

@RunWith(classOf[JUnitRunner])
class ElasticBookDaoTest extends FlatSpec with E2ESpec with Matchers with ScalaFutures {
  override implicit val e2eExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  override implicit def patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))

  val dao = new ElasticBookDao(esClient)
  
  val book = simpleBook.copy(`$schema` = None)
  val cobblers = "cobblers"
  
  "The DAO" should "Find an indexed book by ISBN" in {
    e2e createIndex catalogue index book andAfter { _ =>
      whenReady(dao.getBookByIsbn(book.isbn)) { result =>
        result should equal(Some(book))
      }
    }
  }
  
  it should "return None for an unknown book" in {
    e2e createIndex catalogue index book andAfter { _ =>
      whenReady(dao.getBookByIsbn(cobblers)) { result =>
        result should equal(None)
      }
    }
  }
  
  it should "Find multiple books by ISBN" in {
    e2e createIndex catalogue index book andAfter { _ =>
      whenReady(dao.getBooks(List(book.isbn))) { result =>
        result should equal(List(book))
      }
    }
  }

  it should "omit unknown books from a bulk request" in {
    e2e createIndex catalogue index book andAfter { _ =>
      whenReady(dao.getBooks(List(book.isbn, cobblers))) { result =>
        result should equal(List(book))
      }
    }
  }
}
