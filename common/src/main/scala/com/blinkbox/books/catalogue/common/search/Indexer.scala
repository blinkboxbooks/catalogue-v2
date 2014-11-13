package com.blinkbox.books.catalogue.common.search

import com.blinkbox.books.catalogue.common.Book
import com.sksamuel.elastic4s.{SnowballAnalyzer, WhitespaceAnalyzer, KeywordAnalyzer, ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{BooleanType, StringType, IntegerType}
import com.sksamuel.elastic4s.source.DocumentSource
import com.typesafe.config.Config
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import scala.concurrent.{ExecutionContext, Future}

sealed trait BulkItemResponse
case class Successful(docId: String) extends BulkItemResponse
case class Failure(docId: String, cause: Option[Throwable] = None) extends BulkItemResponse

case class SingleResponse(docId: String)

trait Indexer {
  def index(book: Book): Future[SingleResponse]
  def index(books: Iterable[Book]): Future[Iterable[BulkItemResponse]]
}

class EsIndexer(config: Config, client: ElasticClient)(implicit ec: ExecutionContext) extends Indexer {
  import com.sksamuel.elastic4s.ElasticDsl.{index => esIndex, bulk}

  case class JsonSource(book: Book) extends DocumentSource {
    implicit val formats = Serialization.formats(NoTypeHints)
    def json = Serialization.write(book)
  }

  override def index(book: Book): Future[SingleResponse] = {
    client.execute {
      esIndex into s"${config.getString("search.index.name")}/book" doc JsonSource(book) id book.isbn
    } map { resp =>
      SingleResponse(resp.getId)
    }
  }

  override def index(books: Iterable[Book]): Future[Iterable[BulkItemResponse]] = {
    client.execute {
      bulk(
        books.map { book =>
          esIndex into s"${config.getString("search.index.name")}/book" doc JsonSource(book) id book.isbn
        }.toList: _*
      )
    } map { response =>
      response.getItems.map { item =>
        if(item.isFailed) Failure(item.getId, Some(new RuntimeException(item.getFailureMessage)))
        else Successful(item.getId)
      }
    }
  }
}

case class Schema(config: Config) {
  def classification = "classification" nested(
    "realm" typed StringType analyzer KeywordAnalyzer,
    "id" typed StringType analyzer KeywordAnalyzer
    )

  def uris = "uris" inner(
    "type" typed StringType analyzer KeywordAnalyzer,
    "uri" typed StringType index "not_analyzed",
    "params" typed StringType index "not_analyzed"
    )

  def catalogue = create index s"${config.getString("search.index.name")}" mappings (
    "book" as(
      "title" typed StringType analyzer SnowballAnalyzer,
      "availability" inner(
        "available" typed BooleanType,
        "code" typed StringType analyzer KeywordAnalyzer,
        "extra" typed StringType
        ),
      "isbn" typed StringType analyzer KeywordAnalyzer,
      "regionalRights" inner(
        "GB" typed BooleanType nullValue false,
        "ROW" typed BooleanType nullValue false,
        "WORLD" typed BooleanType nullValue false
        ),
      "publisher" typed StringType analyzer KeywordAnalyzer,
      "media" inner(
        "images" inner(
          classification,
          uris,
          "width" typed IntegerType,
          "height" typed IntegerType,
          "size" typed IntegerType
          ),
        "epubs" inner(
          classification,
          uris,
          "keyFile" typed StringType index "not_analyzed",
          "wordCount" typed IntegerType,
          "size" typed IntegerType
          )
        ),
      "languages" typed StringType analyzer KeywordAnalyzer,
      "descriptions" nested(
        classification,
        "content" typed StringType analyzer SnowballAnalyzer,
        "type" typed StringType analyzer KeywordAnalyzer,
        "author" typed StringType analyzer WhitespaceAnalyzer
        ),
      "subjects" nested(
        "type" typed StringType analyzer KeywordAnalyzer,
        "code" typed StringType analyzer KeywordAnalyzer
        )
      )
    )
}