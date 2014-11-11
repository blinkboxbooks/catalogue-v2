package com.blinkbox.books.catalogue.browser

import com.blinkbox.books.catalogue.common.Book
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl => E}
import org.elasticsearch.action.search.SearchResponse

import scala.concurrent.{ExecutionContext, Future}

trait Suggestion

trait V1SearchService {
  def search(q: String): Future[List[Book]]
  def similar(bookId: Int): Future[List[Book]]
  def suggestions(q: String): Future[List[Suggestion]]
}

class EsV1SearchService(client: ElasticClient)(implicit ec: ExecutionContext) extends V1SearchService {

  private def toBookList(resp: SearchResponse): List[Book] = {
    resp.getHits.hits().map { hit =>
      hit.getSourceAsString
    }
    ???
  }

  override def search(q: String): Future[List[Book]] = client.execute {
    E.search in "catalogue" -> "book" query {
      E.dismax query {
        E.term("title", q) boost 5
        E.term("author", q) boost 4
        E.nested("description") query {
          E.term("content", q)
        } boost 1
      } tieBreaker 0.2
    }
  } map toBookList

  override def similar(bookId: Int): Future[List[Book]] = ???
  override def suggestions(q: String): Future[List[Suggestion]] = ???
}
