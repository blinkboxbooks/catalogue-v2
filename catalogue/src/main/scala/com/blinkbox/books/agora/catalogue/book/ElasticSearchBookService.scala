package com.blinkbox.books.agora.catalogue.book

import java.util.concurrent.Executors
import com.blinkbox.books.logging.DiagnosticExecutionContext

import scala.concurrent.{ExecutionContext, Future}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import scala.util.Success
import org.elasticsearch.common.settings.ImmutableSettings
import org.json4s.jackson.Serialization
import com.blinkbox.books.json.DefaultFormats
import org.elasticsearch.action.get.GetResponse
import com.blinkbox.books.catalogue.common._

import com.blinkbox.books.spray.v1.Link
import com.blinkbox.books.agora.catalogue.app.{ElasticSearchConfig, LinkHelper}

/**
 * ES-based implementation.
 */
class ElasticSearchBookService(esConfig: ElasticSearchConfig, linkHelper: LinkHelper) extends BookService {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))
  implicit val formats = DefaultFormats

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", esConfig.cluster).build()
  val client = ElasticClient.remote(settings, (esConfig.host, esConfig.port))

  private def toBookRepresentation(book: Book): BookRepresentation = {
    BookRepresentation(
      book.isbn,
      book.title,
      book.dates.publish.get,
      isSampleEligible(book),
      extractImages(book),
      Some(generateLinks(book))
    )
  }

  private def extractImages(book: Book) : List[com.blinkbox.books.spray.v1.Image] = {
    def isCoverImage(image: Image) : Boolean = image.classification.filter(c => c.realm.equals("type") && c.id.equals("front_cover")).size > 0
    def extractCoverUri(image : Image) : String = image.uris.filter(u => u.`type`.equals("static")).head.uri

    val coverUri = for (bookImage <- book.media.images; if isCoverImage(bookImage)) yield extractCoverUri(bookImage)
    val coverImage: com.blinkbox.books.spray.v1.Image = new com.blinkbox.books.spray.v1.Image("urn:blinkboxbooks:image:cover",coverUri.head)
    List(coverImage)
  }

  private def isSampleEligible(book: Book) = {
    book.media.epubs.exists(epub => epub.classification.exists(c => c.realm.equals("type") && c.id.equals("sample")) &&
                                    epub.uris.exists(u => u.`type`.equals("static")))
  }

  private def extractSampleLink(book: Book): Link = {
    def isSample(epub: Epub): Boolean = epub.classification.filter(c => c.realm.equals("type") && c.id.equals("sample")).size > 0
    def extractSampleUri(epub: Epub): String = epub.uris.filter(u => u.`type`.equals("static")).head.uri

    val sampleUri = for (epub <- book.media.epubs; if isSample(epub)) yield extractSampleUri(epub)
    linkHelper.linkForSampleMedia(sampleUri.head)
  }

  private def generateLinks(book: Book) : List[Link] = {
    List(
      for (c <- book.contributors) yield linkHelper.linkForContributor(c.id, c.displayName),
      List(linkHelper.linkForBookSynopsis(book.isbn)),
      List(linkHelper.linkForPublisher(123, book.publisher)),
      List(linkHelper.linkForBookPricing(book.isbn)),
      if (isSampleEligible(book)) List(extractSampleLink(book)) else List.empty[Link]).flatten
  }

  override def getBookByIsbn(isbn: String): Future[Option[BookRepresentation]] = {
    client.execute {
      get id isbn from "catalogue/book"
    } map { res =>

      if(res.isSourceEmpty)
          None
        else {
          val book = Serialization.read[Book](res.getSourceAsString)
          Some(toBookRepresentation(book))
        }
    }
  }
}
