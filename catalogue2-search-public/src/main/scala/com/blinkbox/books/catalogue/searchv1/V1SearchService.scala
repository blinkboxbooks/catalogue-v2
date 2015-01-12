package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.{ElasticsearchConfig, IndexEntities => idx}
import com.blinkbox.books.elasticsearch.client.{ElasticClient, SearchResponse, SuggestionOption}
import com.blinkbox.books.spray.{Page, SortOrder}
import com.sksamuel.elastic4s.{ElasticDsl => E, SearchDefinition}
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
  extends V1SearchService {

  import com.blinkbox.books.catalogue.common.Json._
  import com.blinkbox.books.catalogue.searchv1.V1SearchService._
  import com.blinkbox.books.elasticsearch.client.ElasticClientApi._

  val queries = new Queries(searchConfig)

  type IndexResponse = SearchResponse[idx.Book, idx.SuggestionPayload]

  private def toBookSeq(resp: IndexResponse): Option[Seq[Book]] = {
    val respSeq = resp.hits.hits.map(_._source).map { book =>
      Book(book.isbn, book.title, book.contributors.map(_.displayName))
    }.toSeq

    if (respSeq.isEmpty) None else Some(respSeq)
  }

  private def toSpellcheckCompletions(resp: IndexResponse): Option[Seq[String]] = {
    val suggestions: Seq[String] = suggestionOptions(resp, "spellcheck").map(_.text)
    if (suggestions.isEmpty) None else Some(suggestions)
  }

  private def toBookSearchResponse(q: String)(resp: IndexResponse): BookSearchResponse =
    BookSearchResponse(q, toBookSeq(resp), resp.hits.total, toSpellcheckCompletions(resp))

  private def toBookSimilarResponse(resp: IndexResponse): BookSimilarResponse =
    BookSimilarResponse(toBookSeq(resp), resp.hits.total)

  private def suggestionOptions(resp: IndexResponse, suggester: String): Seq[SuggestionOption[idx.SuggestionPayload]] = resp.
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

  private def execute[Req, Resp](req: SearchDefinition): Future[IndexResponse] = client.execute(req.sourceIs[idx.Book].suggestionIs[idx.SuggestionPayload])

  override def search(q: String, page: Page, order: SortOrder): Future[BookSearchResponse] = execute(queries.mainSearch(q, page, order)).map(toBookSearchResponse(q))

  override def similar(bookId: BookId, page: Page): Future[BookSimilarResponse] = execute(queries.similarBooks(bookId, page)).map(toBookSimilarResponse)

  override def suggestions(q: String, count: Int): Future[BookCompletionResponse] = execute(queries.suggestions(q, count)).map(toCompletionResponse)
}
