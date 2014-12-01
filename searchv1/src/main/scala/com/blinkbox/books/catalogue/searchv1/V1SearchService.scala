package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.IndexEntities.{SuggestionItem, SuggestionPayload, SuggestionType}
import com.blinkbox.books.catalogue.common.{ElasticsearchConfig, IndexEntities => idx}
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl => E}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry
import org.json4s.jackson.Serialization

import scala.collection.convert.Wrappers.{JIteratorWrapper, JListWrapper}
import scala.concurrent.{ExecutionContext, Future}

case class BookId(value: String) extends AnyVal

object V1SearchService {
  case class Book(id: String, title: String, authors: List[String])
  case class Suggestion(id: String, title: String, `type`: String, authors: Option[List[String]])
}

trait V1SearchService {
  import com.blinkbox.books.catalogue.searchv1.V1SearchService._

  def search(q: String): Future[Iterable[Book]]
  def similar(bookId: BookId): Future[Iterable[Book]]
  def suggestions(q: String): Future[Iterable[Suggestion]]
}

class EsV1SearchService(searchConfig: ElasticsearchConfig, client: ElasticClient)(implicit ec: ExecutionContext) extends V1SearchService {
  import com.blinkbox.books.catalogue.common.Json._
  import com.blinkbox.books.catalogue.searchv1.V1SearchService._

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
          E.termQuery("isbn", q) boost 4,
          // Query for the title - give precedence to title that match including stop-words
          E.dismax query(
            E.matchPhrase("title", q) boost 1 slop 1,
            E.matchPhrase("titleWithStopwords", q) boost 2 slop 1
          ) tieBreaker 0 boost 3, // No tie breaker as it would be pointless in this case
          E.nestedQuery("contributors") query (
            E.matchPhrase("contributors.displayName", q) slop 1
          ) boost 2,
          E.nestedQuery("descriptions") query (
            E.matchPhrase("descriptions.content", q) slop 1
          ) boost 1
        ) tieBreaker 0.2
      } filter {
        E.termFilter("distributionStatus.usable", true)
      }
    }
  } map toBookIterable

  override def similar(bookId: BookId): Future[Iterable[Book]] = client execute {
    searchIn("book") query {
      E.morelikeThisQuery("title", "descriptionContents") minTermFreq 1 maxQueryTerms 12 ids bookId.value
    } filter {
      E.termFilter("distributionStatus.usable", true)
    }
  } map toBookIterable

  override def suggestions(q: String): Future[Iterable[Suggestion]] = client execute {
    searchIn("catalogue") suggestions (
      E.suggest using(E.completion) as "autoComplete" on q from "autoComplete"
    ) filter {
      E.termFilter("distributionStatus.usable", true)
    }
  } map toSuggestionIterable
}
