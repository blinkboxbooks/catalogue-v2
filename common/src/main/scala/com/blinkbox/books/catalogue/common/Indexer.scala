package com.blinkbox.books.catalogue.common

import scala.concurrent.Future

trait IndexerTypes {
  type IndexCommand
  type BulkIndexCommand

  type SuccessfulCommand
  type FailedCommand
}

trait Indexer[Types <: IndexerTypes] {
  type IndexResponse = Either[Types#FailedCommand, Types#SuccessfulCommand]

  def index[T](content: T)(implicit cnt: IndexContent[T, Types]): Future[IndexResponse]
  def index[T](content: Iterable[T])(implicit cnt: IndexContent[T, Types]): Future[Iterable[IndexResponse]]
}

trait IndexContent[ContentType, T <: IndexerTypes] {
  def single(content: ContentType): T#IndexCommand
  def bulk(content: Iterable[ContentType]): T#BulkIndexCommand
}
