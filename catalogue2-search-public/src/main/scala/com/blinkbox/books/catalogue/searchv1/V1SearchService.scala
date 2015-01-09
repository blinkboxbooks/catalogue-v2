package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.{ ElasticsearchConfig, IndexEntities => idx }
import com.blinkbox.books.catalogue.common.search.{ ElasticSearchFutures, ElasticSearchSupport }
import com.blinkbox.books.elasticsearch.client.{ ElasticClient, ElasticRequest, SearchResponse }
import com.blinkbox.books.spray.{ Page, SortOrder }
import com.sksamuel.elastic4s.{ ElasticDsl => E, MoreLikeThisQueryDefinition }
import org.elasticsearch.search.suggest.Suggest.Suggestion
import scala.concurrent.{ ExecutionContext, Future }

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
    suggestions: Option[Seq[String]] = None,
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
  extends V1SearchService with ElasticSearchSupport with ElasticSearchFutures {

  import com.blinkbox.books.catalogue.common.Json._
  import com.blinkbox.books.catalogue.searchv1.V1SearchService._
  import com.blinkbox.books.elasticsearch.client.ElasticClientApi._

  private val DistributionStatusDocType = "distribution-status"

  type IndexResponse = SearchResponse[idx.Book, idx.SuggestionPayload]

  private def toBookSeq(resp: IndexResponse): Option[Seq[Book]] = {
    val respSeq = resp.hits.hits.map(_._source).map { book =>
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

  private def toSpellcheckCompletions(resp: IndexResponse): Option[Seq[String]] = {
    val suggestions: Seq[String] = suggestionOptions(resp, "spellcheck").map(_.text)
    if (suggestions.isEmpty) None else Some(suggestions)
  }

  private def toBookSearchResponse(q: String)(resp: IndexResponse): BookSearchResponse =
    BookSearchResponse(q, toBookSeq(resp), resp.hits.total, toSpellcheckCompletions(resp))

  private def toBookSimilarResponse(resp: IndexResponse): BookSimilarResponse =
    BookSimilarResponse(toBookSeq(resp), resp.hits.total)

  private def suggestionOptions(resp: IndexResponse, suggester: String) = resp.
    suggest.
    getOrElse(Map.empty).
    get(suggester).
    getOrElse(Seq.empty).
    flatMap(_.options)

  private def toCompletionResponse(resp: IndexResponse): BookCompletionResponse = BookCompletionResponse(
    suggestionOptions(resp, "autoComplete").
      flatMap(_.payload).
      collect {
        case idx.SuggestionPayload(idx.SuggestionType.Book, idx.SuggestionItem.Book(isbn, title, authors)) =>
          Completion(isbn, title, "book", Some(authors))
        case idx.SuggestionPayload(idx.SuggestionType.Contributor, idx.SuggestionItem.Contributor(id, name)) =>
          Completion(id, name, "author", None)
      }
  )

  private def searchIn(`type`: String) = E.search in s"${searchConfig.indexName}/${`type`}"

  private def execute[Req, Resp](req: E.SearchDefinition): Future[IndexResponse] = client.execute(req.sourceIs[idx.Book].suggestionIs[idx.SuggestionPayload])

  override def search(q: String, page: Page, order: SortOrder): Future[BookSearchResponse] = execute {
    (paginate(page.offset, page.count) {
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
                  E.dismax query (
                    E.matchPhrase("contributors.displayName", q) slop 10 boost 10,
                    E.matches("contributors.displayName", q) operator "or" boost 5
                  ) tieBreaker 0
                ) boost 2,
                    E.nestedQuery("descriptions") query (
                      E.matchPhrase("descriptions.content", q) slop 100
                    ) boost 1
            ) tieBreaker 0.2
          } filter {
            E.hasChildFilter(DistributionStatusDocType) filter E.termFilter("usable", true)
          }
        }
      }
    } suggestions (E.suggest using (E.phrase) as "spellcheck" on q from "spellcheck" size 1 maxErrors 3))
  }.recoverException.map(toBookSearchResponse(q))

  override def similar(bookId: BookId, page: Page): Future[BookSimilarResponse] = execute {
    (searchIn("book") query {
      similarBooksQuery(bookId.value)
    } filter {
      E.hasChildFilter(DistributionStatusDocType) filter E.termFilter("usable", true)
    } limit page.count from page.offset)
  }.recoverException.map(toBookSimilarResponse)

  override def suggestions(q: String, count: Int): Future[BookCompletionResponse] = execute {
    (searchIn("catalogue") suggestions (
      E.suggest using (E.completion) as "autoComplete" on q from "autoComplete" size count
    ) filter {
        E.hasChildFilter(DistributionStatusDocType) filter E.termFilter("usable", true)
      } limit 0) // We don't want search results, only suggestions
  }.recoverException.map(toCompletionResponse)
}
