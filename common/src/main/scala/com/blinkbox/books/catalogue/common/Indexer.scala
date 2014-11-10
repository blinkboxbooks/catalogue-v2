package com.blinkbox.books.catalogue.common

import com.sksamuel.elastic4s.{ElasticClient, IndexDefinition, ElasticDsl => E}
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.index.IndexResponse

import scala.collection.convert.Wrappers.JMapWrapper
import scala.concurrent.{ExecutionContext, Future}

trait Indexer {
  import Indexer._

  type IndexCommand

  type SuccessfulCommand
  type FailedCommand

  type CommandResponse = Either[FailedCommand, SuccessfulCommand]

  type BulkCommand = Iterable[IndexCommand]
  type BulkCommandResponse = Iterable[CommandResponse]

  def index[T: IndexContent](content: T): IndexCommand

  def execute(cmd: IndexCommand): Future[CommandResponse]

  def bulk(cmd: BulkCommand): Future[BulkCommandResponse]
}

object Indexer {
  trait IndexContent[ContentType] {
    type IdType
    def path: String
    def id(content: ContentType): Option[IdType]
    def fields(content: ContentType): Map[String, Any]
  }
}
