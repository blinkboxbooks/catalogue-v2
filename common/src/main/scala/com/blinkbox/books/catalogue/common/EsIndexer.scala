package com.blinkbox.books.catalogue.common

import com.sksamuel.elastic4s.{BulkDefinition, ElasticClient, IndexDefinition}
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.index.{IndexResponse => EsIndexResponse}

import scala.concurrent.{ExecutionContext, Future}

object EsIndexer {
  case class EsDocumentMeta(index: String, `type`: String, id: String, version: Long)
  case class EsSuccessfulIndex(meta: EsDocumentMeta)
  case class EsFailedIndex(message: String, code: Int, meta: EsDocumentMeta) extends RuntimeException(message)
}

class EsIndexer(client: ElasticClient)(implicit ec: ExecutionContext) extends Indexer {
  import com.blinkbox.books.catalogue.common.EsIndexer._

  type IndexCommand = IndexDefinition
  type BulkIndexCommand = BulkDefinition

  type SuccessfulCommand = EsSuccessfulIndex
  type FailedCommand = Throwable

  private def createMeta(r: BulkItemResponse): EsDocumentMeta = EsDocumentMeta(r.getIndex, r.getType, r.getId, r.getVersion)
  private def createMeta(r: EsIndexResponse): EsDocumentMeta = EsDocumentMeta(r.getIndex, r.getType, r.getId, r.getVersion)

  override def index[T](content: T)(implicit cnt: IndexContent[T, this.type]): Future[IndexResponse] =
    client.execute(cnt.single(content)).map { resp =>
      Right(EsSuccessfulIndex(createMeta(resp)))
    } recover { case ex => Left(ex)}

  override def index[T](content: Iterable[T])(implicit cnt: IndexContent[T, this.type]): Future[Iterable[IndexResponse]] =
    client.execute(cnt.bulk(content)).map { resp =>
      resp.getItems.map { item =>
        val meta = createMeta(item)

        if (item.isFailed) Left(EsFailedIndex(item.getFailureMessage, item.getFailure.getStatus.getStatus, meta))
        else Right(EsSuccessfulIndex(meta))
      }
    }
}

