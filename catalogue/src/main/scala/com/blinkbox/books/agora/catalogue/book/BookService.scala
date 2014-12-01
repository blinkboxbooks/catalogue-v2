package com.blinkbox.books.agora.catalogue.book

import com.blinkbox.books.agora.catalogue.app.LinkHelper
import scala.concurrent.Future
import com.blinkbox.books.spray.v1.{Link => V1Link, Image => V1Image, ListPage, pageLink2Link}
import com.blinkbox.books.logging.DiagnosticExecutionContext
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common._
import com.blinkbox.books.spray.{Page, Paging}

trait BookDao {
  def getBookByIsbn(isbn: String): Future[Option[Book]]
  def getBooks(isbns: List[String]): Future[List[Book]]
}

trait BookService {
  def getBookByIsbn(isbn: String): Future[Option[BookRepresentation]]
  def getBookSynopsis(isbn: String): Future[Option[BookSynopsis]]
  def getBooks(isbns: Iterable[String], page: Page): Future[ListPage[BookRepresentation]]
}

class DefaultBookService(dao: BookDao, linkHelper: LinkHelper) extends BookService {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))

  private def toBookRepresentation(book: Book): BookRepresentation = {
    val media = getWithException(book.media, "'media' missing.")
    val publicationDate = book.dates.map(_.publish).flatten
    BookRepresentation(
      isbn = book.isbn,
      title = book.title,
      publicationDate = getWithException(publicationDate, "'publicationDate' missing."),
      sampleEligible = isSampleEligible(media),
      images = extractImages(media),
      links = Some(generateLinks(book, media))
    )
  }

  private def extractImages(media: Media) : List[V1Image] = {
    def isCoverImage(image: Image): Boolean = image.classification.count(c => c.realm.equals("type") && c.id.equals("front_cover")) > 0
    def extractCoverUri(image : Image) : String = image.uris.filter(u => u.`type`.equals("static")).head.uri

    val coverUri = for(bookImage <- media.images; if isCoverImage(bookImage)) yield extractCoverUri(bookImage)
    List(new V1Image("urn:blinkboxbooks:image:cover", coverUri.head))
  }

  private def isSampleEligible(media: Media): Boolean =
    media.epubs.exists{ epub =>
      epub.classification.exists(c => c.realm.equals("type") && c.id.equals("sample")) &&
        epub.uris.exists(u => u.`type`.equals("static"))
    }

  private def extractSampleLink(media: Media): V1Link = {
    def isSample(epub: Epub): Boolean = epub.classification.count(c => c.realm.equals("type") && c.id.equals("sample")) > 0
    def extractSampleUri(epub: Epub): String = epub.uris.filter(u => u.`type`.equals("static")).head.uri

    val sampleUri = for (epub <- media.epubs; if isSample(epub)) yield extractSampleUri(epub)
    linkHelper.linkForSampleMedia(sampleUri.head)
  }

  private def generateLinks(book: Book, media: Media) : List[V1Link] = {
    // TODO - nasty creating all these lists then flattening the result, better way? (other than using mutable list?)
    // list of producers and to fold over an accumulator (which would be the final result containing all the links)
    List(
      for (c <- book.contributors) yield linkHelper.linkForContributor(c.id, c.displayName),
      List(linkHelper.linkForBookSynopsis(book.isbn)),
      List(linkHelper.linkForPublisher(123, book.publisher.get)), // TODO - publisher ID!!!
      List(linkHelper.linkForBookPricing(book.isbn)),
      if (isSampleEligible(media)) List(extractSampleLink(media)) else List.empty[V1Link]
    ).flatten
  }

  private def toBookSynopsis(book: Book): BookSynopsis = {
    def isMainDescription(description: OtherText): Boolean = description.classification.exists(c => c.realm.equals("source") && c.id.equals("Main description"))

    val synopsisText = for (desc <- book.descriptions; if isMainDescription(desc)) yield desc.content
    BookSynopsis(book.isbn, synopsisText.head)
  }
  
  override def getBookByIsbn(isbn: String): Future[Option[BookRepresentation]] = {
    dao.getBookByIsbn(isbn).map(_.map(book => toBookRepresentation(book)))
  }

  override def getBookSynopsis(isbn: String): Future[Option[BookSynopsis]] = {
    dao.getBookByIsbn(isbn).map(_.map(book => toBookSynopsis(book)))
  }
  
  override def getBooks(isbns: Iterable[String], page: Page): Future[ListPage[BookRepresentation]] = {
    // Construct links
    val params = Some(isbns.toSeq.map(isbn => ("id", isbn)))
    val links = if (isbns.size > page.count) {
      val paging = Paging.links(Some(isbns.size), page.offset, page.count, /*s"$serviceBaseUrl$path"*/"url", params, includeSelf=false)
      Some(paging.toList.map(pageLink2Link))
    } else None
    
    // Build slice of results
    val slice = isbns.slice(page.offset, page.offset + page.count).toList    
    val books = dao.getBooks(slice).map(_.map(book => toBookRepresentation(book)))
    
    // Paginate
    books.map(results => ListPage(isbns.size, page.offset, slice.size, results, links))
  }
  
  /*
  def bookList(list: java.util.List[BookDTO], mediaMap: Option[Map[String, Map[BookMediaType, BookMediaDTO]]], linkHelper: LinkHelper): List[Book] =
    list.map(m => model(m, mediaMap.flatMap(_.get(m.getIsbn)), linkHelper)).toList
  
  private def getMediaMap(isbn: String*): Option[Map[String, Map[BookMediaType, BookMediaDTO]]] = {
    if (isbn.nonEmpty) Option(bookMediaService.getMediaForIsbns(isbn).toMap.mapValues(_.toMap)) else None
  }
  */
  
  private def getWithException[T](from: Option[T], exceptionMessage: String): T = from.getOrElse(throw new IllegalArgumentException(exceptionMessage))
}
