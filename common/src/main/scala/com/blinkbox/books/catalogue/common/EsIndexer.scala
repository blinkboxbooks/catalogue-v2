package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.Indexer.IndexContent
import com.sksamuel.elastic4s.{ElasticClient, IndexDefinition, ElasticDsl => E}
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.index.IndexResponse

import scala.concurrent.{ExecutionContext, Future}

object EsIndexer {
  case class EsDocumentMeta(index: String, `type`: String, id: String, version: Long)
  case class EsSuccessfulIndex(meta: EsDocumentMeta)
  case class EsFailedIndex(message: String, code: Int, meta: EsDocumentMeta) extends RuntimeException(message)
}

class EsIndexer(client: ElasticClient)(implicit ec: ExecutionContext) extends Indexer {
  import EsIndexer._

  type IndexCommand = IndexDefinition

  type SuccessfulCommand = EsSuccessfulIndex
  type FailedCommand = Throwable

  private def createMeta(r: BulkItemResponse): EsDocumentMeta = EsDocumentMeta(r.getIndex, r.getType, r.getId, r.getVersion)
  private def createMeta(r: IndexResponse): EsDocumentMeta = EsDocumentMeta(r.getIndex, r.getType, r.getId, r.getVersion)

  private def attachId[T](definition: IndexDefinition, id: Option[T]): IndexDefinition =
    id.map(definition id _).getOrElse(definition)

  override def index[T](content: T)(implicit idx: IndexContent[T]): IndexCommand =
    attachId(E.index into idx.path fields idx.fields(content), idx.id(content))

  override def execute(cmd: IndexCommand): Future[CommandResponse] = client execute cmd map { r =>
    Right(EsSuccessfulIndex(createMeta(r)))
  } recover { case ex => Left(ex)}

  override def bulk(cmd: BulkCommand): Future[BulkCommandResponse] = client execute E.bulk(cmd.toSeq: _*) map { resp =>
    resp.getItems.map { item =>
      val meta = createMeta(item)

      if (item.isFailed) Left(EsFailedIndex(item.getFailureMessage, item.getFailure.getStatus.getStatus, meta))
      else Right(EsSuccessfulIndex(meta))
    }
  }
}

