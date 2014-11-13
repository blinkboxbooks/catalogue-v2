package com.blinkbox.books.catalogue.browser

import com.blinkbox.books.catalogue.common.Book
import com.blinkbox.books.json.DefaultFormats
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl => E}
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry
import org.json4s.jackson.Serialization
import org.elasticsearch.action.search.SearchResponse

import scala.collection.convert.Wrappers.JIteratorWrapper
import scala.concurrent.{ExecutionContext, Future}

trait Suggestion

case class BookId(value: Int) extends AnyVal

trait V1SearchService {
  def search(q: String): Future[Iterable[Book]]
  def similar(bookId: BookId): Future[Iterable[Book]]
  def suggestions(q: String): Future[Iterable[Suggestion]]
}

class EsV1SearchService(client: ElasticClient)(implicit ec: ExecutionContext) extends V1SearchService {
  implicit val formats = DefaultFormats

  private def toBookIterable(resp: SearchResponse): Iterable[Book] =
    resp.getHits.hits().map { hit =>
      Serialization.read[Book](hit.getSourceAsString)
    }

  private def toSuggestionIterable(resp: SearchResponse): Iterable[Suggestion] =
    JIteratorWrapper(resp.
      getSuggest.
      getSuggestion[Suggest.Suggestion[Entry]]("autoComplete").
      iterator).
      map(s => Serialization.read[Suggestion](s.getText.string)).
      toIterable

  override def search(q: String): Future[Iterable[Book]] = client.execute {
    E.search in "catalogue/book" query {
      E.dismax query {
        E.term("title", q) boost 5
        E.term("author", q) boost 4
        E.nested("description") query {
          E.term("content", q)
        } boost 1
      } tieBreaker 0.2
    }
  } map toBookIterable

  override def similar(bookId: BookId): Future[Iterable[Book]] = client.execute {
    E.morelike id bookId.value in "catalogue/book"
  } map toBookIterable

  override def suggestions(q: String): Future[Iterable[Suggestion]] = client.execute {
    E.search in "catalogue" suggestions (
      E.suggest as "autoComplete" on q from "autoComplete"
    )
  } map toSuggestionIterable
}
