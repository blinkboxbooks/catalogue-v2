package com.blinkbox.books.catalogue.common

import com.sksamuel.elastic4s.{ElasticClient, IndexDefinition, ElasticDsl => E}
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.index.IndexResponse

import scala.collection.convert.Wrappers.JMapWrapper
import scala.concurrent.{ExecutionContext, Future}

trait Indexer {
  type IndexCommand
  type BulkIndexCommand

  type SuccessfulCommand
  type FailedCommand

  type IndexResponse = Either[FailedCommand, SuccessfulCommand]

  def index[T](content: T)(implicit cnt: IndexContent[T, this.type]): Future[IndexResponse]
  def index[T](content: Iterable[T])(implicit cnt: IndexContent[T, this.type]): Future[Iterable[IndexResponse]]
}

trait IndexContent[ContentType, I <: Indexer] {
  def single(content: ContentType): I#IndexCommand
  def bulk(content: Iterable[ContentType]): I#BulkIndexCommand
}

