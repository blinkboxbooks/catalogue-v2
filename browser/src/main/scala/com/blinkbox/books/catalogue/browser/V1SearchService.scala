package com.blinkbox.books.catalogue.browser

import com.blinkbox.books.catalogue.common.IndexEntities.{ContributorPayload, BookPayload}
import com.blinkbox.books.json.DefaultFormats
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl => E}
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.term.TermSuggestion.Entry
import org.json4s.jackson.Serialization
import org.elasticsearch.action.search.SearchResponse
import com.blinkbox.books.catalogue.common.{IndexEntities => idx}

import scala.collection.convert.Wrappers.JIteratorWrapper
import scala.concurrent.{ExecutionContext, Future}

case class BookId(value: String) extends AnyVal

trait V1SearchService {
  case class Book(id: String, title: String, authors: List[String])
  case class Suggestion(id: String, title: String, `type`: String, authors: Option[List[String]])

  def search(q: String): Future[Iterable[Book]]
  def similar(bookId: BookId): Future[Iterable[Book]]
  def suggestions(q: String): Future[Iterable[Suggestion]]
}

class EsV1SearchService(client: ElasticClient)(implicit ec: ExecutionContext) extends V1SearchService {
  implicit val formats = DefaultFormats

  private def toBookIterable(resp: SearchResponse): Iterable[Book] =
    resp.getHits.hits().map { hit =>
      val book = Serialization.read[idx.Book](hit.getSourceAsString)
      Book(book.isbn, book.title, book.contributors.map(_.displayName))
    }

  private def toSuggestionIterable(resp: SearchResponse): Iterable[Suggestion] =
    JIteratorWrapper(resp.
      getSuggest.
      getSuggestion[Suggest.Suggestion[Entry]]("autoComplete").
      iterator).
      map(s => Serialization.read[idx.SuggestionResponse](s.getText.string)).
      flatMap { s =>
        s.options.map(_.payload).collect {
          case BookPayload(isbn, title, authors) => Suggestion(isbn, title, "book", Some(authors))
          case ContributorPayload(id, name) => Suggestion(id, name, "contributor", None)
        }
      }.toIterable

  override def search(q: String): Future[Iterable[Book]] = client.execute {
    E.search in "catalogue/book" filter {
      E.termFilter("distribute", true)
    } query {
      E.dismax query (
        E.term("title", q) boost 5,
        E.nested("contributors") query (
          E.term("contributors.displayName", q)
        ) boost 4,
        E.nested("descriptions") query {
          E.term("descriptions.content", q)
        } boost 1
      ) tieBreaker 0.2
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
