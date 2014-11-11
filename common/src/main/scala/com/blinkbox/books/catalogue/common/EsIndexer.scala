package com.blinkbox.books.catalogue.common

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{BooleanType, IntegerType, StringType}
import com.sksamuel.elastic4s._
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.index.{IndexResponse => EsIndexResponse}

import scala.concurrent.{ExecutionContext, Future}

object Schema {
  def classification = "classification" nested (
    "realm" typed StringType analyzer KeywordAnalyzer,
    "id" typed StringType analyzer KeywordAnalyzer
    )

  def uris =  "uris" inner (
    "type" typed StringType analyzer KeywordAnalyzer,
    "uri" typed StringType index "not_analyzed",
    "params" typed StringType index "not_analyzed"
    )

  val catalogue = create index "catalogue" mappings (
    "book" as (
      "title" typed StringType analyzer SnowballAnalyzer,
      "availability" inner (
        "available" typed BooleanType,
        "code" typed StringType analyzer KeywordAnalyzer,
        "extra" typed StringType
        ),
      "isbn" typed StringType analyzer KeywordAnalyzer,
      "regionalRights" inner (
        "GB" typed BooleanType nullValue false,
        "ROW" typed BooleanType nullValue false,
        "WORLD" typed BooleanType nullValue false
        ),
      "publisher" typed StringType analyzer KeywordAnalyzer,
      "media" inner (
        "images" inner (
          classification,
          uris,
          "width" typed IntegerType,
          "height" typed IntegerType,
          "size" typed IntegerType
          ),
        "epubs" inner (
          classification,
          uris,
          "keyFile" typed StringType index "not_analyzed",
          "wordCount" typed IntegerType,
          "size" typed IntegerType
          )
        ),
      "languages" typed StringType analyzer KeywordAnalyzer,
      "descriptions" nested (
        classification,
        "content" typed StringType analyzer SnowballAnalyzer,
        "type" typed StringType analyzer KeywordAnalyzer,
        "author" typed StringType analyzer WhitespaceAnalyzer
        ),
      "subjects" nested (
        "type" typed StringType analyzer KeywordAnalyzer,
        "code" typed StringType analyzer KeywordAnalyzer
        )
      )
    )
}

object EsIndexer {
  case class EsDocumentMeta(index: String, `type`: String, id: String, version: Long)
  case class EsSuccessfulIndex(meta: EsDocumentMeta)
  case class EsFailedIndex(message: String, code: Int, meta: EsDocumentMeta) extends RuntimeException(message)
}

trait EsIndexerTypes extends IndexerTypes {
  import EsIndexer._

  type IndexCommand = IndexDefinition
  type BulkIndexCommand = BulkDefinition

  type SuccessfulCommand = EsSuccessfulIndex
  type FailedCommand = Throwable
}

class EsIndexer(client: ElasticClient)(implicit ec: ExecutionContext) extends Indexer[EsIndexerTypes] {
  import com.blinkbox.books.catalogue.common.EsIndexer._

  private def createMeta(r: BulkItemResponse): EsDocumentMeta = EsDocumentMeta(r.getIndex, r.getType, r.getId, r.getVersion)
  private def createMeta(r: EsIndexResponse): EsDocumentMeta = EsDocumentMeta(r.getIndex, r.getType, r.getId, r.getVersion)

  override def index[T](content: T)(implicit cnt: IndexContent[T, EsIndexerTypes]): Future[IndexResponse] =
    client.execute(cnt.single(content)).map { resp =>
      Right(EsSuccessfulIndex(createMeta(resp)))
    } recover { case ex => Left(ex)}

  override def index[T](content: Iterable[T])(implicit cnt: IndexContent[T, EsIndexerTypes]): Future[Iterable[IndexResponse]] =
    client.execute(cnt.bulk(content)).map { resp =>
      resp.getItems.map { item =>
        val meta = createMeta(item)

        if (item.isFailed) Left(EsFailedIndex(item.getFailureMessage, item.getFailure.getStatus.getStatus, meta))
        else Right(EsSuccessfulIndex(meta))
      }
    }
}

