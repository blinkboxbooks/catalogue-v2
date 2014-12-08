package com.blinkbox.books.agora.catalogue.book

import com.blinkbox.books.catalogue.common.Events.Book
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.mockito.Mockito._
import com.blinkbox.books.agora.catalogue.app.LinkHelper
import com.blinkbox.books.test.MockitoSyrup
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import com.blinkbox.books.catalogue.common._
import org.joda.time.DateTime
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import com.blinkbox.books.spray.v1.{Image => SprayImage, Link}
import com.blinkbox.books.spray.{Page, SortOrder}
import java.net.URL
import java.net.URI
import spray.http.HttpRequest

@RunWith(classOf[JUnitRunner])
class DefaultBookServiceTest extends FlatSpecLike with Matchers with MockitoSyrup with ScalaFutures {
  val isbn = "isbn"
  val now = DateTime.now
  
  val image = Image(
    List(Classification("type", "front_cover")),
    List(Uri(`type`="static", uri="image", params = None)),
    1, 2, 3
  )
  
  val sample = Epub(
    List(Classification("type", "sample")),
    List(Uri(`type`="static", uri="sample", params = None)),
    None,
    1, 2
  )
  
  val desc = OtherText(
    List(Classification("source", "Main description")),
    "synopsis",
    null,
    None
  )
  
  val book = Book(
    sequenceNumber = 1,
    `$schema` = None,
    classification = List.empty[Classification],
    isbn = isbn,
    format = None,
    title = "title",
    subtitle = Option.empty[String],
    contributors = List(Contributor("role", "id", "contributor", "sortName")),
    availability = None,
    dates = Some(Dates(publish=Some(now), announce=None)),
    descriptions = List(desc),
    reviews = List.empty[OtherText],
    languages = List.empty[String],
    originalLanguages = List.empty[String],
    supplyRights = None,
    salesRights = None,
    publisher = Some("publisher"),
    imprint = None,
    prices = List.empty[Price],
    statistics = None,
    subjects = List.empty[Subject],
    series = None,
    related = List.empty[Related],
    media = Some(Media(List(sample), List(image))),
    distributionStatus = DistributionStatus(usable = true, List.empty[String]),
    source = Source(deliveredAt = Option.empty,
      uri = Option.empty,
      fileName = Option.empty,
      contentType = Option.empty,
      role = Option.empty,
      username = "some-username",
      system = Option.empty,
      processedAt = Option.empty)
  )
  
  val expectedImage = SprayImage("urn:blinkboxbooks:image:cover", "image")
  val link = mock[Link]
  val expected = BookRepresentation("isbn", "title", now, true, List(expectedImage), Some(List(link, link, link, link, link)))
  
  val linkHelper = mock[LinkHelper]
  when(linkHelper.externalUrl).thenReturn(spray.http.Uri("catalogue"))
  when(linkHelper.bookPath).thenReturn("/books")
  
  val dao = mock[BookDao]
  val service = new DefaultBookService(dao, linkHelper)
  
  private def addBook(book: Book) = when(dao.getBookByIsbn(isbn)).thenReturn(Future.successful(Some(book)))
  
  it should "return a book representation for an existing book" in {
    addBook(book)
    when(linkHelper.linkForContributor("id", "contributor")).thenReturn(link)
    when(linkHelper.linkForBookSynopsis(isbn)).thenReturn(link)
    when(linkHelper.linkForPublisher(123, "publisher")).thenReturn(link) // TODO - publisher ID
    when(linkHelper.linkForBookPricing(isbn)).thenReturn(link)
    when(linkHelper.linkForSampleMedia("sample")).thenReturn(link)
    whenReady(service.getBookByIsbn(isbn)) { result =>
      assert(Some(expected) == result)
    }
  }
  
  it should "have an empty sample link if there is no sample epub" in {
    addBook(book.copy(media=Some(Media(List(), List(image)))))
    whenReady(service.getBookByIsbn(isbn)) { result =>
      verifyZeroInteractions(linkHelper.linkForSampleMedia("sample"))
    }
  }
  
  it should "return nothing if the book does not exist" in {
    when(dao.getBookByIsbn(isbn)).thenReturn(Future.successful(None))
    whenReady(service.getBookByIsbn(isbn)) { result =>
      assert(None == result)
    }
  }
  
  it should "fail if the book has no contributors" in {
    addBook(book.copy(contributors=List()))
    service.getBookByIsbn(isbn)
  }
  
  it should "fail if the book has no media" in {
    addBook(book.copy(media=None))
    service.getBookByIsbn(isbn)
  }
  
  it should "fail if the book has no publication date" in {
    addBook(book.copy(dates=None))
    service.getBookByIsbn(isbn)
  }
  
  it should "return the synopsis for an existing book" in {
    addBook(book)
    whenReady(service.getBookSynopsis(isbn)) { result =>
      assert(result == Some(BookSynopsis("urn:blinkboxbooks:id:synopsis:isbn", isbn, "synopsis")))
    }
  }
  
  it should "return an empty synopsis if the book does not exist" in {
    when(dao.getBookByIsbn(isbn)).thenReturn(Future.successful(None))
    whenReady(service.getBookSynopsis(isbn)) { result =>
      assert(None == result)
    }
  }
  
  it should "return bulk books" in {
    val isbns = List.fill(7)("isbn")
    when(dao.getBooks(isbns)).thenReturn(Future.successful(List.fill(7)(book)))
    whenReady(service.getBooks(isbns, Page(0, 999))) { listPage =>
      assert(7 == listPage.numberOfResults, "Total number of books")
      assert(0 == listPage.offset)
      assert(7 == listPage.count)
      assert(7 == listPage.items.size, "Page size")
      assert(None == listPage.links)
    }
  }
  
  it should "return paginated bulk books results" in {
    when(dao.getBooks(List("2"))).thenReturn(Future.successful(List(book)))
    whenReady(service.getBooks(List("1", "2", "3"), Page(1, 1))) { listPage =>
      assert(3 == listPage.numberOfResults, "Total number of books")
      assert(1 == listPage.offset)
      assert(1 == listPage.count)
      assert(1 == listPage.items.size, "Page size")
      assert(List(expected) == listPage.items)
      // TODO - verify links
      //val prev = Link("prev", "catalogue/books?id=1&id=2&id=3&count=1&offset=0", None, None)
      //val next = Link("next", "catalogue/books?id=1&id=2&id=3&count=1&offset=2", None, None)
      //assert(Some(Set(prev, next)) == listPage.links.map(_.toSet))
    }
  }

  it should "return books given a contributor" in {
    when(dao.getBooksByContributor("id", None, None, 0, 1, "title", true)).thenReturn(Future.successful(BookList(List(book), 2)))
    val page = Page(0, 1)
    val order = SortOrder("title", true)
    whenReady(service.getBooksByContributor("id", None, None, page, order)) { listPage =>
      assert(2 == listPage.numberOfResults, "Total number of books")
      assert(0 == listPage.offset)
      assert(1 == listPage.count)
      assert(1 == listPage.items.size, "Page size")
      assert(List(expected) == listPage.items)
      // TODO - verify links
      //assert(Some(Set(Link("next", "catalogue/contributor?contributor=id&order=title&"))) == listPages.links.map(_.toSet))
    }
  }
  
  it should "return related books for a given ISBN" in {
    when(dao.getRelatedBooks("isbn", 0, 1)).thenReturn(Future.successful(BookList(List(book), 2)))
    val page = Page(0, 1)
    whenReady(service.getRelatedBooks("isbn", page)) { listPage =>
      assert(2 == listPage.numberOfResults, "Total number of books")
      assert(0 == listPage.offset)
      assert(1 == listPage.count)
      assert(1 == listPage.items.size, "Page size")
      assert(List(expected) == listPage.items)
      // TODO - verify links
      //assert(Some(Set("")) == listPages.links.map(_.toSet))
    }
  }
}
