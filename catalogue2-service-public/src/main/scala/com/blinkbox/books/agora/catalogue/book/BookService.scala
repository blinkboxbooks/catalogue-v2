package com.blinkbox.books.agora.catalogue.book

import com.blinkbox.books.agora.catalogue.app.LinkHelper
import scala.concurrent.Future
import com.blinkbox.books.spray.v1.{Link => V1Link, Image => V1Image, ListPage, pageLink2Link}
import com.blinkbox.books.logging.DiagnosticExecutionContext
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common._
import com.blinkbox.books.spray.{Page, Paging, SortOrder}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class BookList(books: List[Book], total: Int)

trait BookDao {
  def getBookByIsbn(isbn: String): Future[Option[Book]]
  def getBooks(isbns: List[String]): Future[List[Book]]
  def getBooksByContributor(id: String, minDate: Option[DateTime], maxDate: Option[DateTime], offset: Int, count: Int, sortField: String, sortDescending: Boolean): Future[BookList]
  def getRelatedBooks(isbn: String, offset: Int, count: Int): Future[BookList]
}

trait BookService {
  def getBookByIsbn(isbn: String): Future[Option[BookRepresentation]]
  def getBookSynopsis(isbn: String): Future[Option[BookSynopsis]]
  def getBooks(isbns: Iterable[String], page: Page): Future[ListPage[BookRepresentation]]
  def getBooksByContributor(id: String, minPubDate: Option[DateTime], maxPubDate: Option[DateTime], page: Page, order: SortOrder): Future[ListPage[BookRepresentation]]
  def getRelatedBooks(isbn: String, page: Page): Future[ListPage[BookRepresentation]]
}

object BookService {
  val idParam = "id"
  val contributorParam = "contributor"
  val minPubDateParam = "minPublicationDate"
  val maxPubDateParam = "maxPublicationDate"
  val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
}

class DefaultBookService(dao: BookDao, linkHelper: LinkHelper) extends BookService {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))

  private def isRealm(list: List[Classification], realm: String = "type", id: String) = list.exists(c => c.realm.equals(realm) && c.id.equals(id))
  private val isStatic = (uri: Uri) => uri.`type`.equals("static")

  private def toBookRepresentation(book: Book): BookRepresentation = {
    def getWithException[T](from: Option[T], exceptionMessage: String): T = from.getOrElse(throw new IllegalArgumentException(exceptionMessage))
    val media = getWithException(book.media, "'media' missing.")
    val publicationDate = book.dates.map(_.publish).flatten
    BookRepresentation(
      isbn = book.isbn,
      title = book.title,
      publicationDate = getWithException(publicationDate, "'publicationDate' missing."),
      sampleEligible = getSampleUri(media).isDefined,
      images = getImages(media),
      links = Some(generateLinks(book, media))
    )
  }
  
  private def getImages(media: Media): List[V1Image] = {
    media.images
      .filter(image => isRealm(image.classification, id="front_cover"))
      .flatMap(image => image.uris)
      .filter(isStatic)
      .map(uri => V1Image("urn:blinkboxbooks:image:cover", uri.uri))
  }

  private def getSampleUri(media: Media): Option[Uri] = {
    media.epubs
      .filter(epub => isRealm(epub.classification, id="sample"))
      .flatMap(epub => epub.uris)
      .filter(isStatic)
      .headOption
  }
  
  private def generateLinks(book: Book, media: Media) : List[V1Link] = {
    val bookLinks = List(
      linkHelper.linkForBookSynopsis(book.isbn),
      linkHelper.linkForPublisher(123, book.publisher.get), // TODO - publisher ID!!!
      linkHelper.linkForBookPricing(book.isbn)
    )
    val contributorLinks = for (c <- book.contributors) yield linkHelper.linkForContributor(c.id, c.displayName)
    val sampleLink = getSampleUri(media).map(uri => linkHelper.linkForSampleMedia(uri.uri))
    bookLinks ++ contributorLinks ++ sampleLink
  }
  
  private def toListPage(books: List[Book], total: Int, page: Page, path: String, params: Option[Seq[(String, String)]]): ListPage[BookRepresentation] = {
    val links = if(total > page.count) {
      val pageLinks = Paging.links(Some(total), page.offset, page.count, linkHelper.externalUrl.path.toString + path, params, includeSelf=false)
      Some(pageLinks.toList.map(pageLink2Link))
    }
    else {
      None
    }    
    ListPage(total, page.offset, books.size, books.map(toBookRepresentation), links)
  }
  
  override def getBookByIsbn(isbn: String): Future[Option[BookRepresentation]] = {
    dao.getBookByIsbn(isbn).map(_.map(book => toBookRepresentation(book)))
  }

  override def getBookSynopsis(isbn: String): Future[Option[BookSynopsis]] = {
    def toSynopsis(book: Book) = {
      val isMainDescription = (desc: OtherText) => isRealm(desc.classification, "source", "Main description")
      val list = for(desc <- book.descriptions; if isMainDescription(desc)) yield desc.content
      if(list.isEmpty) "" else list.head
    }
    dao.getBookByIsbn(isbn).map(_.map(book => BookSynopsis(book.isbn, toSynopsis(book))))
  }
  
  override def getBooks(isbns: Iterable[String], page: Page): Future[ListPage[BookRepresentation]] = {
    val slice = isbns.slice(page.offset, page.offset + page.count).toList
    val params = Some(isbns.toSeq.map(isbn => (BookService.idParam, isbn)))
    dao.getBooks(slice) map { books => toListPage(books, isbns.size, page, linkHelper.bookPath, params) }
  }

  override def getBooksByContributor(id: String, minPubDate: Option[DateTime], maxPubDate: Option[DateTime], page: Page, order: SortOrder): Future[ListPage[BookRepresentation]] = {
    def dateQueryParam(param: String, date: Option[DateTime]):Option[(String, String)] = date.map(d => (param, BookService.dateTimeFormat.print(d)))
    val params = Seq(
      Some((BookService.contributorParam, id)),
      dateQueryParam(BookService.minPubDateParam, minPubDate),
      dateQueryParam(BookService.maxPubDateParam, maxPubDate)
    )
    val res = dao.getBooksByContributor(id, minPubDate, maxPubDate, page.offset, page.count, order.field, order.desc)
    res map { bookList => toListPage(bookList.books, bookList.total, page, linkHelper.bookPath, Some(params.flatten ++ order.asQueryParams)) }      
  }
  
  override def getRelatedBooks(isbn: String, page: Page): Future[ListPage[BookRepresentation]] = {
    val res = dao.getRelatedBooks(isbn, page.offset, page.count)
    res map { bookList => toListPage(bookList.books, bookList.total, page, s"${linkHelper.bookPath}/${isbn}/related", None) }
  }
}
