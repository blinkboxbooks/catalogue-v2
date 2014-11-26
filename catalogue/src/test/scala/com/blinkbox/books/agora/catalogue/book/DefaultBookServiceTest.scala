package com.blinkbox.books.agora.catalogue.book

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.mockito.Matchers._
import org.mockito.Mockito._
import com.blinkbox.books.agora.catalogue.app.LinkHelper
import com.blinkbox.books.test.MockitoSyrup
import scala.concurrent.Future
import com.blinkbox.books.catalogue.common.Book
import org.scalatest.concurrent.ScalaFutures
import scala.util.Success
import com.blinkbox.books.catalogue.common._
import org.joda.time.DateTime
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import com.blinkbox.books.spray.v1.{Image => SprayImage, Link}

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
    source = Source(now, None, None, None, "NONE-ROLE", "NONE-USERNAME", None, None)
  )
  
  val dao = mock[BookDao]
  val linkHelper = mock[LinkHelper]
  val service = new DefaultBookService(dao, linkHelper)
  
  private def addBook(book: Book) = when(dao.getBookByIsbn(isbn)).thenReturn(Future.successful(Some(book)))
  
  it should "return a book representation for an existing book" in {
    addBook(book)
    val link = mock[Link]
    when(linkHelper.linkForContributor("id", "contributor")).thenReturn(link)
    when(linkHelper.linkForBookSynopsis(isbn)).thenReturn(link)
    when(linkHelper.linkForPublisher(123, "publisher")).thenReturn(link) // TODO - publisher ID
    when(linkHelper.linkForBookPricing(isbn)).thenReturn(link)
    when(linkHelper.linkForSampleMedia("sample")).thenReturn(link)
    whenReady(service.getBookByIsbn(isbn)) { result =>
      val image = SprayImage("urn:blinkboxbooks:image:cover", "image")
      val rep = BookRepresentation("isbn", "title", now, true, List(image), Some(List(link, link, link, link, link)))
      assert(Some(rep) == result)
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
}
