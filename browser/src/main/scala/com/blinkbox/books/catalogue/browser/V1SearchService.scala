package com.blinkbox.books.catalogue.browser

import com.blinkbox.books.catalogue.common.IndexEntities.{SuggestionItem, SuggestionPayload, SuggestionType}
import com.blinkbox.books.catalogue.common.{SearchConfig, IndexEntities => idx}
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl => E}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry
import org.json4s.jackson.Serialization

import scala.collection.convert.Wrappers.{JIteratorWrapper, JListWrapper}
import scala.concurrent.{ExecutionContext, Future}

case class BookId(value: String) extends AnyVal

trait V1SearchService {
  case class Book(id: String, title: String, authors: List[String])
  case class Suggestion(id: String, title: String, `type`: String, authors: Option[List[String]])

  def search(q: String): Future[Iterable[Book]]
  def similar(bookId: BookId): Future[Iterable[Book]]
  def suggestions(q: String): Future[Iterable[Suggestion]]
}

class EsV1SearchService(searchConfig: SearchConfig, client: ElasticClient)(implicit ec: ExecutionContext) extends V1SearchService {
  import com.blinkbox.books.catalogue.common.Json._

  private def toBookIterable(resp: SearchResponse): Iterable[Book] =
    resp.getHits.hits().map { hit =>
      val book = Serialization.read[idx.Book](hit.getSourceAsString)
      Book(book.isbn, book.title, book.contributors.map(_.displayName))
    }

  def toSuggestion(payload: SuggestionPayload): Suggestion =
    if (payload.`type` == SuggestionType.Book) {
      val bookItem = payload.item.asInstanceOf[SuggestionItem.Book]
      Suggestion(bookItem.isbn, bookItem.title, "book", Some(bookItem.authors))
    } else {
      val contributorItem = payload.item.asInstanceOf[SuggestionItem.Contributor]
      Suggestion(contributorItem.id, contributorItem.displayName, "author", None)
    }

  private def toSuggestionIterable(resp: SearchResponse): Iterable[Suggestion] =
    (for {
      autoComplete <- JIteratorWrapper(resp.getSuggest.getSuggestion[Suggest.Suggestion[Entry]]("autoComplete").iterator)
      option <- JListWrapper(autoComplete.getOptions)
      payload = Serialization.read[SuggestionPayload](option.getPayloadAsString)
    } yield toSuggestion(payload)).toIterable

  private def searchIn(`type`: String) = E.search in s"${searchConfig.indexName}/${`type`}"

  override def search(q: String): Future[Iterable[Book]] = client execute {
    searchIn("book") query {
      E.filteredQuery query {
        E.dismax query(
          E.matchPhrase("title", q) boost 5 slop 1,
          E.nested("contributors") query (
            E.matchPhrase("contributors.displayName", q) slop 1
            ) boost 4,
          E.nested("descriptions") query {
            E.matchPhrase("descriptions.content", q) slop 1
          } boost 1
          ) tieBreaker 0.2
      } filter {
        E.termFilter("distribute", true)
      }
    }
  } map toBookIterable

  override def similar(bookId: BookId): Future[Iterable[Book]] = client execute {
    searchIn("book") query {
      E.morelikeThisQuery("title", "descriptionContents") minTermFreq 1 maxQueryTerms 12 ids bookId.value
    }
  } map toBookIterable

  override def suggestions(q: String): Future[Iterable[Suggestion]] = client execute {
    searchIn("catalogue") suggestions (
      E.suggest using(E.completion) as "autoComplete" on q from "autoComplete"
    )
  } map toSuggestionIterable
}
