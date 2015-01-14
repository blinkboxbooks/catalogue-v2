package com.blinkbox.books.agora.catalogue.book

import com.blinkbox.books.catalogue.common.e2e.HttpEsSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.time.{Millis, Span}
import org.scalatest.concurrent.ScalaFutures
import spray.testkit.ScalatestRouteTest
import scala.concurrent.Await
import com.blinkbox.books.catalogue.common._
import com.blinkbox.books.catalogue.common.BookFixtures.simpleBook
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ElasticBookDaoTest extends FlatSpec
  with HttpEsSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with ScalatestRouteTest{

  override implicit def patienceConfig = PatienceConfig(timeout = Span(5000, Millis), interval = Span(500, Millis))

  val dao = new ElasticBookDao(esClient, "catalogue/book")

  def createBook(bookIsbn: String, bookTitle: String, date: Int, price: Int, contributor: String) = simpleBook.copy(
    `$schema` = None,
    isbn=bookIsbn,
    title=bookTitle,
    dates=Some(Dates(Some(new DateTime(date)), None)),
    prices=List(simpleBook.prices.head.copy(amount=price)),
    contributors=List(simpleBook.contributors.head.copy(id=contributor, sortName=bookTitle))
  )    

  val books = List(
    createBook("1", "The Last Book", 1, 3, "one"),
    createBook("2", "A Book", 2, 2, "one"),
    createBook("3", "Another Book", 3, 1, "two")
  )
  
  val contributor = "one"
  val contributorBooks = books.filter(_.contributors.exists(_.id == contributor))
    
  val cobblers = "cobblers"
  val sortField = "title"
  val count = 25

  def catalogueIndex = e2e createIndex catalogue
  def populateIndex = catalogueIndex index(books.toSeq: _*)

  override def beforeAll(): Unit = {
    super.beforeAll()
    Await.ready(populateIndex.state, 10.seconds)
  }

  "The DAO" should "find an indexed book by ISBN" in {
    whenReady(dao.getBookByIsbn("1")) { result =>
      result should equal(books.find(_.isbn == "1"))
    }
  }
  
  it should "return None for an unknown book" in {
    whenReady(dao.getBookByIsbn(cobblers)) { result =>
      result should equal(None)
    }
  }
  
  it should "find multiple books by ISBN" in {
    whenReady(dao.getBooks(books.map(_.isbn))) { result =>
      result.toSet should equal(books.toSet)
    }
  }

  it should "omit unknown books from a bulk request" in {
    whenReady(dao.getBooks(List("1", cobblers, "2", cobblers, "3"))) { result =>
      result.toSet should equal(books.toSet)
    }
  }
  
  it should "find books for a given contributor" in {
    whenReady(dao.getBooksByContributor(contributor, None, None, 0, count, sortField, sortDescending = true)) { result =>
      result should equal(BookList(books.filter(_.isbn != "3"), 2))
    }
  }
  
  it should "return an empty results set for an unknown contributor" in {
    whenReady(dao.getBooksByContributor("cobblers", None, None, 0, count, sortField, sortDescending = true)) { result =>
      result should equal(BookList(List(), 0))
    }
  }
  
  it should "limit the number of results to the specified count" in {
     whenReady(dao.getBooksByContributor(contributor, None, None, 0, 1, sortField, sortDescending = true)) { result =>
       assert(result.books.size == 1)
       assert(result.total == 2)
    }
  }
  
  it should "start the results at the given offset" in {
    whenReady(dao.getBooksByContributor(contributor, None, None, 1, count, sortField, sortDescending = true)) { result =>
       assert(result.books.size == 1)
       assert(result.total == 2)
    }
  }
  
  it should "sort books by title" in {
    whenReady(dao.getBooksByContributor(contributor, None, None, 0, count, "title", sortDescending = false)) { result =>
      result should equal(BookList(contributorBooks.sortBy(_.title), 2))
    }
  }

  it should "sorts books in reverse order" in {
    whenReady(dao.getBooksByContributor(contributor, None, None, 0, count, "title", sortDescending = true)) { result =>
      result should equal(BookList(contributorBooks.sortBy(_.title).reverse, 2))
    }
  }

  it should "sort books by publication date" in {
    whenReady(dao.getBooksByContributor(contributor, None, None, 0, count, "publication_date", sortDescending = false)) { result =>
      result should equal(BookList(contributorBooks.sortBy(_.isbn), 2))
    }
  }

  it should "sort books by price" in {
    whenReady(dao.getBooksByContributor(contributor, None, None, 0, count, "price", sortDescending = true)) { result =>
      result should equal(BookList(contributorBooks.sortBy(_.isbn), 2))
    }
  }

  it should "sort books by contributor name" in {
    whenReady(dao.getBooksByContributor(contributor, None, None, 0, count, "author", sortDescending = true)) { result =>
      result should equal(BookList(contributorBooks.sortBy(_.isbn), 2))
    }
  }

  it should "find books by a given contributor within the specified date-range" in {
    whenReady(dao.getBooksByContributor(contributor, Some(new DateTime(2)),
       Some(new DateTime(2)), 0, count, "publication_date", sortDescending = true)) { result =>
      result should equal(BookList(contributorBooks.filter(_.isbn == "2"), 1))
    }
  }

  it should "fail for an invalid page offset" in {
    intercept[IllegalArgumentException] {
      dao.getBooksByContributor("contributor", None, None, -1, count, sortField, sortDescending = true)
    }
  }

  it should "fail for an invalid page size" in {
    intercept[IllegalArgumentException] {
      dao.getBooksByContributor("contributor", None, None, 0, 0, sortField, sortDescending = true)
    }
  }
  
  it should "fail for an invalid sort order" in {
    intercept[IllegalArgumentException] {
      dao.getBooksByContributor("contributor", None, None, 0, count, cobblers, sortDescending = true)
    }
  }
  
  it should "return related books for a given ISBN" in {
    whenReady(dao.getRelatedBooks("1", 0, count)) { result =>
      assert(result.books.size == 2)
      assert(result.total == 2)
    }
  }
  
  it should "limit the number of related books to the given count" in {
    whenReady(dao.getRelatedBooks("1", 0, 1)) { result =>
      assert(result.books.size == 1)
      assert(result.total == 2)
    }
  }
  
  it should "start the related books at the given offset" in {
    whenReady(dao.getRelatedBooks("1", 1, 1)) { result =>
      assert(result.books.size == 1)
      assert(result.total == 2)
    }
  }
  
  it should "return an empty set of related books for an unknown ISBN" in {
    whenReady(dao.getRelatedBooks(cobblers, 0, count)) { result =>
      result should equal(BookList(List(), 0))
    }
  }

  it should "return an undistributed book by ISBN" in {
    val ISBN = "1234567890123"
    val book = simpleBook.copy(isbn = ISBN)
    val undistribute = UndistributeFixtures.simpleWith(ISBN).copy(usable = false)

    catalogueIndex index (book, undistribute) andAfter { _ =>
      whenReady(dao.getBookByIsbn(ISBN)) { result =>
        val Some(book) = result
        assert(book.isbn == ISBN)
      }
    }
  }
}
