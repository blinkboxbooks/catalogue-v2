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
    availability = Some(BookAvailability(None, Some(Availability(true, "code", "extra")), None, None, None)),
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
  val expected = BookRepresentation("isbn", "title", now, sampleEligible = true, List(expectedImage), Some(List(link, link, link, link, link)))
  
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

  /*
  it should "have an empty sample link if the book is undistributed" in {
    addBook(book)
    when(dao.getDistributionStatus(isbn)).thenReturn(Future.successful(Some(Undistribute(isbn, false, List(), 1))))
    whenReady(service.getBookByIsbn(isbn)) { result =>
      verifyZeroInteractions(linkHelper.linkForSampleMedia("sample"))
    }
  }
  */
  
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
  
  private def checkLinks(links: Option[List[Link]], expected: Map[String, String]) = {
    // Order actual links by name
    val map = links.getOrElse(fail("No links")).map(link => (link.rel, link.href)).toMap
    
    expected.foreach { expectedLink =>
      // Convert to URI
      val actualUri = spray.http.Uri(map.getOrElse(expectedLink._1, fail(s"Expected link: ${expectedLink._1}")))
      val expectedUri = spray.http.Uri(expectedLink._2)
      
      // Check path
      assert(expectedUri.path == actualUri.path)

      // Check query parameters (note there can be multiple parameters with the same key)
      expectedUri.query.toSeq.foreach { param =>
        assert(actualUri.query.getAll(param._1).contains(param._2), s"Expected query param: ${param._1}")
      }

      // Check correct number of query parameters
      assert(actualUri.query.toSeq.size == expectedUri.query.toSeq.size, s"Mis-matched number of query parameters: $actualUri")
    }
    
    // Check correct number of links
    assert(map.size == expected.size, s"Mis-matched number of links:$map")
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
      checkLinks(listPage.links,Map(
        "prev" -> "catalogue/books?id=1&id=2&id=3&count=1&offset=0",
        "next"-> "catalogue/books?id=1&id=2&id=3&count=1&offset=2"
      ))
    }
  }

  it should "return books given a contributor" in {
    when(dao.getBooksByContributor("id", None, None, 0, 1, "title", sortDescending = true)).thenReturn(Future.successful(BookList(List(book), 2)))
    val page = Page(0, 1)
    val order = SortOrder("title", desc = true)
    whenReady(service.getBooksByContributor("id", None, None, page, order)) { listPage =>
      assert(2 == listPage.numberOfResults, "Total number of books")
      assert(0 == listPage.offset)
      assert(1 == listPage.count)
      assert(1 == listPage.items.size, "Page size")
      assert(List(expected) == listPage.items)
      checkLinks(listPage.links,Map(
        "next"-> "catalogue/books?contributor=id&order=title&desc=true&count=1&offset=1"
      ))
    }
  }
  
  it should "return related books for a given ISBN" in {
    when(dao.getRelatedBooks("isbn", 2, 3)).thenReturn(Future.successful(BookList(List.fill(3)(book), 7)))
    val page = Page(2, 3)
    whenReady(service.getRelatedBooks("isbn", page)) { listPage =>
      assert(7 == listPage.numberOfResults, "Total number of books")
      assert(2 == listPage.offset)
      assert(3 == listPage.count)
      assert(3 == listPage.items.size, "Page size")
      assert(List.fill(3)(expected) == listPage.items)
      assert(None == listPage.links)
    }
  }
  
  it should "return an empty results set for an ISBN with no related books" in {
    when(dao.getRelatedBooks("isbn", 0, 999)).thenReturn(Future.successful(BookList(List(), 0)))
    whenReady(service.getRelatedBooks("isbn", Page(0, 999))) { listPage =>
      assert(0 == listPage.numberOfResults, "Total number of books")
      assert(0 == listPage.offset)
      assert(999 == listPage.count)
      assert(0 == listPage.items.size, "Page size")
      assert(listPage.items.isEmpty)
      assert(None == listPage.links)
    }    
  }
}
