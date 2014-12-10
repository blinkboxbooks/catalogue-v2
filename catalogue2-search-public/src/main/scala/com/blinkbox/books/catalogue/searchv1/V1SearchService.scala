package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.{ElasticsearchConfig, IndexEntities => idx}
import com.blinkbox.books.catalogue.common.IndexEntities.{SuggestionItem, SuggestionPayload, SuggestionType}
import com.blinkbox.books.catalogue.common.search.{ElasticSearchFutures, ElasticSearchSupport}
import com.blinkbox.books.spray.{Page, SortOrder}
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl => E, MoreLikeThisQueryDefinition}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.Suggest.Suggestion
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.{Entry => CompletionEntry}
import org.elasticsearch.search.suggest.phrase.PhraseSuggestion.{Entry => PhraseEntry}
import org.json4s.jackson.{Serialization => Json}
import scala.collection.convert.Wrappers.{JIteratorWrapper, JListWrapper}
import scala.concurrent.{ExecutionContext, Future}

case class BookId(value: String) extends AnyVal

object V1SearchService {
  val bookSuggestionType = "urn:blinkbox:schema:suggestion:book"
  val contributorSuggestionType = "urn:blinkbox:schema:suggestion:contributor"
  val bookSearchResponseType = "urn:blinkbox:schema:search"
  val bookSimilarResponseType = "urn:blinkbox:schema:search:similar"
  val bookCompletionResponseType = "urn:blinkbox:schema:list"

  case class Book(id: String, title: String, authors: List[String])
  case class Completion(id: String, title: String, `type`: String, authors: Option[List[String]])

  /*
   * TODO: Try using ExplicitTypeHints rather than the `type` parameter as in
   * https://git.mobcastdev.com/Agora/library-service/blob/master/src/main/scala/com/blinkbox/books/agora/LibraryApi.scala
   */
  trait PaginableResponse { def numberOfResults: Long }

  case class BookSearchResponse(
    id: String,
    books: Option[Seq[Book]],
    numberOfResults: Long,
    suggestions: Seq[String] = Seq.empty,
    `type`: String = bookSearchResponseType
  ) extends PaginableResponse

  case class BookSimilarResponse(
    books: Option[Seq[Book]],
    numberOfResults: Long,
    `type`: String = bookSimilarResponseType
  ) extends PaginableResponse

  case class BookCompletionResponse(items: Seq[Completion], `type`: String = bookCompletionResponseType)
}

trait V1SearchService {
  import com.blinkbox.books.catalogue.searchv1.V1SearchService._

  def search(q: String, page: Page, order: SortOrder): Future[BookSearchResponse]
  def similar(bookId: BookId, page: Page): Future[BookSimilarResponse]
  def suggestions(q: String, count: Int): Future[BookCompletionResponse]
}

class EsV1SearchService(searchConfig: ElasticsearchConfig, client: ElasticClient)(implicit ec: ExecutionContext)
  extends V1SearchService with ElasticSearchSupport with ElasticSearchFutures{

  import com.blinkbox.books.catalogue.common.Json._
  import com.blinkbox.books.catalogue.searchv1.V1SearchService._

  private def toBookSeq(resp: SearchResponse): Option[Seq[Book]] = {
    val respSeq = resp.getHits.hits().map { hit =>
      val book = Json.read[idx.Book](hit.getSourceAsString)
      Book(book.isbn, book.title, book.contributors.map(_.displayName))
    }.toSeq

    if (respSeq.isEmpty) None else Some(respSeq)
  }
  
  override val SortFieldMapping = Map(
    "relevance" -> "_score",
    "author" -> "contributors.sortName",
    "popularity" -> "_score", // TODO - not yet implemented    
    "price" -> "prices.amount",
    "publication_date" -> "dates.publish"
  )

  // Define some types trying to alleviate the Java recursive-variant-existential-types insanity of ES
  private type OptionType = Suggestion.Entry.Option
  private type EntryType[O <: OptionType] = Suggestion.Entry[O]
  private type SuggestionType[E <: EntryType[_]] = Suggestion[E]

  private def getSuggestionIterator[E <: EntryType[_]](resp: SearchResponse, name: String): Iterator[E] =
    Option(resp.getSuggest.getSuggestion[SuggestionType[E]](name)).fold[Iterator[E]](Iterator.empty)(es => JIteratorWrapper(es.iterator))

  private def toSpellcheckCompletions(resp: SearchResponse): Seq[String] =
    (for {
      spellcheck <- getSuggestionIterator[PhraseEntry](resp, "spellcheck")
      option <- JListWrapper(spellcheck.getOptions)
    } yield option.getText.string).toSeq

  private def toBookSearchResponse(q: String)(resp: SearchResponse): BookSearchResponse =
    BookSearchResponse(q, toBookSeq(resp), resp.getHits.getTotalHits, toSpellcheckCompletions(resp))

  private def toBookSimilarResponse(resp: SearchResponse): BookSimilarResponse =
    BookSimilarResponse(toBookSeq(resp), resp.getHits.getTotalHits)

  private def toCompletionResponse(resp: SearchResponse): BookCompletionResponse = BookCompletionResponse(toCompletionSeq(resp))

  def toCompletion(payload: SuggestionPayload): Completion =
    if (payload.`type` == SuggestionType.Book) {
      val bookItem = payload.item.asInstanceOf[SuggestionItem.Book]
      Completion(bookItem.isbn, bookItem.title, bookSuggestionType, Some(bookItem.authors))
    } else {
      val contributorItem = payload.item.asInstanceOf[SuggestionItem.Contributor]
      Completion(contributorItem.id, contributorItem.displayName, contributorSuggestionType, None)
    }

  private def toCompletionSeq(resp: SearchResponse): Seq[Completion] =
    (for {
      autoComplete <- getSuggestionIterator[CompletionEntry](resp, "autoComplete")
      option <- JListWrapper(autoComplete.getOptions)
      payload = Json.read[SuggestionPayload](option.getPayloadAsString)
    } yield toCompletion(payload)).toSeq

  private def searchIn(`type`: String) = E.search in s"${searchConfig.indexName}/${`type`}"

  override def search(q: String, page: Page, order: SortOrder): Future[BookSearchResponse] =
    client.execute{
      paginate(page.offset, page.count) {
        sortBy(order.field, order.desc) {
          searchIn("book") query {
            E.filteredQuery query {
              E.dismax query (
                E.termQuery("isbn", q) boost 4,
                // Query for the title - give precedence to title that match including stop-words
                E.dismax query (
                  E.matchPhrase("title", q) boost 1 slop 10,
                  E.matchPhrase("titleSimple", q) boost 2 slop 10
                ) tieBreaker 0 boost 3, // No tie breaker as it would be pointless in this case
                  E.nestedQuery("contributors") query (
                    E.matchPhrase("contributors.displayName", q) slop 10
                  ) boost 2,
                    E.nestedQuery("descriptions") query (
                      E.matchPhrase("descriptions.content", q) slop 100
                    ) boost 1
              ) tieBreaker 0.2
            } filter {
              E.hasChildFilter("distribution-status") filter E.termFilter("usable", true)
            }
          }
        }
      } suggestions (E.suggest using (E.phrase) as "spellcheck" on q from "titleSimple" size 1)
    }.recoverException.map(toBookSearchResponse(q))

  override def similar(bookId: BookId, page: Page): Future[BookSimilarResponse] =
    client.execute {
      searchIn("book") query {
        similarBooksQuery(bookId.value)
      } filter {
        E.hasChildFilter("distribution-status") filter E.termFilter("usable", true)
      } limit page.count from page.offset
    }.recoverException.map(toBookSimilarResponse)

  override def suggestions(q: String, count: Int): Future[BookCompletionResponse] = client.execute {
    searchIn("catalogue") suggestions (
      E.suggest using (E.completion) as "autoComplete" on q from "autoComplete" size count
    ) filter {
        E.hasChildFilter("distribution-status") filter E.termFilter("usable", true)
      } limit 0 // We don't want search results, only suggestions
  }.recoverException.map(toCompletionResponse)
}
