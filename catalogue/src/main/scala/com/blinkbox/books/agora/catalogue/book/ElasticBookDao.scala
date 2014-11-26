package com.blinkbox.books.agora.catalogue.book

import java.util.concurrent.Executors
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.logging.DiagnosticExecutionContext
import scala.concurrent.{ExecutionContext, Future}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.json4s.jackson.Serialization
import com.blinkbox.books.json.DefaultFormats

class ElasticBookDao(client: ElasticClient) extends BookDao {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))
  implicit val formats = DefaultFormats

  override def getBookByIsbn(isbn: String): Future[Option[Book]] = {
    client.execute {
      get id isbn from "catalogue/book"
    } map { res =>
      if(res.isSourceEmpty)
          None
        else {
          Some(Serialization.read[Book](res.getSourceAsString))
        }
    }
  }
}
