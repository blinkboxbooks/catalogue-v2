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

@RunWith(classOf[JUnitRunner])
class ElasticBookDaoTest extends FlatSpec with E2ESpec with Matchers with ScalaFutures {
  override implicit val e2eExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  override implicit def patienceConfig = PatienceConfig(timeout = Span(5000, Millis), interval = Span(500, Millis))

  val dao = new ElasticBookDao(esClient, "catalogue/book")

  private def createBook(id: Int): Book = simpleBook.copy(
    `$schema` = None,
    isbn=id.toString,
    title=id.toString,
    dates=Some(Dates(Some(new DateTime(id)), None)),
    prices=List(simpleBook.prices.head.copy(amount=id))
  )
  
  val one = createBook(1)
  val two = createBook(2)
  
  val cobblers = "cobblers"
  val sortField = "title"
  val count = 50
  val contributor = one.contributors.head.id

  private def related(book: Book): Related = Related(None, None, Some(book.isbn))
  
  val related = one.copy(isbn="other", related=List(related(one), related(two)))

  "The DAO" should "find an indexed book by ISBN" in {
    e2e createIndex catalogue index one andAfter { _ =>
      whenReady(dao.getBookByIsbn(one.isbn)) { result =>
        result should equal(Some(one))
      }
    }
  }
  
  it should "return None for an unknown book" in {
    e2e createIndex catalogue index one andAfter { _ =>
      whenReady(dao.getBookByIsbn(cobblers)) { result =>
        result should equal(None)
      }
    }
  }
  
  it should "find multiple books by ISBN" in {
    e2e createIndex catalogue index(one, two) andAfter { _ =>
      whenReady(dao.getBooks(List(one.isbn, two.isbn))) { result =>
        result should equal(List(one, two))
      }
    }
  }

  it should "omit unknown books from a bulk request" in {
    e2e createIndex catalogue index one andAfter { _ =>
      whenReady(dao.getBooks(List(one.isbn, cobblers))) { result =>
        result should equal(List(one))
      }
    }
  }
  
  it should "find books for a given contributor (with default sort order)" in {
    e2e createIndex catalogue index(one, two) andAfter { _ =>
      whenReady(dao.getBooksByContributor(contributor, 0, count, sortField, false)) { result =>
        result should equal(BookList(List(one, two), 2))
      }
    }
  }
  
  it should "return an empty results set for an unknown contributor" in {
    e2e createIndex catalogue andAfter { _ =>
      whenReady(dao.getBooksByContributor("cobblers", 0, count, sortField, true)) { result =>
        result should equal(BookList(List(), 0))
      }
    }
  }
  
  it should "limit the number of results to the specified count" in {
    e2e createIndex catalogue index(one, two) andAfter { _ =>
      whenReady(dao.getBooksByContributor(contributor, 0, 1, sortField, false)) { result =>
        result should equal(BookList(List(one), 2))
      }
    }
  }
  
  it should "start the results at the given offset" in {
    e2e createIndex catalogue index(one, two) andAfter { _ =>
      whenReady(dao.getBooksByContributor(contributor, 1, count, sortField, false)) { result =>
        result should equal(BookList(List(two), 2))
      }
    }
  }
  
  it should "return books in reverse order with ascending parameter specified" in {
    e2e createIndex catalogue index(one, two) andAfter { _ =>
      whenReady(dao.getBooksByContributor(contributor, 0, count, sortField, true)) { result =>
        result should equal(BookList(List(two, one), 2))
      }
    }
  }
  
  it should "sort books by publication date" in {
    e2e createIndex catalogue index(one, two) andAfter { _ =>
      whenReady(dao.getBooksByContributor(contributor, 0, count, "publication_date", true)) { result =>
        result should equal(BookList(List(two, one), 2))
      }
    }
  }

  it should "sort books by price" in {
    e2e createIndex catalogue index(one, two) andAfter { _ =>
      whenReady(dao.getBooksByContributor(contributor, 0, count, "price", true)) { result =>
        result should equal(BookList(List(two, one), 2))
      }
    }
  }
  
  it should "sort books by contributor name" in {
    def createContributorBook(book: Book, name: String): Book = {
      val c = book.contributors.head.copy(sortName=name)
      book.copy(contributors=List(c))
    }
    val first = createContributorBook(two, "aaa")
    val second = createContributorBook(one, "bbb")
    e2e createIndex catalogue index(first, second) andAfter { _ =>
      whenReady(dao.getBooksByContributor(contributor, 0, count, "author", true)) { result =>
        result should equal(BookList(List(second, first), 2))
      }
    }
  }

  it should "fail for an invalid page offset" in {
    intercept[IllegalArgumentException] {
      dao.getBooksByContributor("contributor", -1, count, sortField, true)
    }
  }

  it should "fail for an invalid page size" in {
    intercept[IllegalArgumentException] {
      dao.getBooksByContributor("contributor", 0, 0, sortField, true)
    }
  }
  
  it should "fail for an invalid sort order" in {
    intercept[IllegalArgumentException] {
      dao.getBooksByContributor("contributor", 0, count, cobblers, true)
    }
  }
  
  it should "return related books for a given ISBN" in {
    e2e createIndex catalogue index(related, one, two) andAfter { _ =>
      whenReady(dao.getRelatedBooks(related.isbn, 0, count)) { result =>
        result should equal(BookList(List(one, two), 2))
      }
    }
  }
  
  it should "limit the number of related books to the given count" in {
    e2e createIndex catalogue index(related, one, two) andAfter { _ =>
      whenReady(dao.getRelatedBooks(related.isbn, 0, 1)) { result =>
        result should equal(BookList(List(one), 2))
      }
    }
  }
  
  it should "start the related books at the given offset" in {
    e2e createIndex catalogue index(related, one, two) andAfter { _ =>
      whenReady(dao.getRelatedBooks(related.isbn, 1, count)) { result =>
        result should equal(BookList(List(two), 2))
      }
    }
  }
  
  it should "return an empty set of related books for an unknown ISBN" in {
    e2e createIndex catalogue andAfter { _ =>
      whenReady(dao.getRelatedBooks(cobblers, 0, count)) { result =>
        result should equal(BookList(List(), 0))
      }
    }
  }
}
