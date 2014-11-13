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
import com.blinkbox.books.catalogue.common.Book

/**
 * ES-based implementation.
 */
class ElasticSearchBookService extends BookService {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))
  implicit val formats = DefaultFormats

  // TODO - config
  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", "elasticsearch_chris").build()
  val client = ElasticClient.remote(settings, ("localhost", 9300))

  private def toBookRepresentation(book: Book): BookRepresentation = {
    BookRepresentation(
      s"urn:blinkboxbooks:id:book:${book.isbn}",
      book.isbn,
      book.title,
      "13/11/2014", // TODO - publicationDate, from which field?
      true, // TODO - sampleEligible, derive from epub section? or always true?
      List(), // TODO - from images section
      None // TODO - links, use LinkHelper
    )
  }

  override def getBookByIsbn(isbn: String): Future[Option[BookRepresentation]] = {
    client.execute {
      get id isbn from "catalogue/books"
    } map { res =>
      println("=====================")
      println(res.getSourceAsString)
      if(res.isSourceEmpty)
          None
        else {
          val book = Serialization.read[Book](res.getSourceAsString)
          Some(toBookRepresentation(book))
        }
    }
  }
}
