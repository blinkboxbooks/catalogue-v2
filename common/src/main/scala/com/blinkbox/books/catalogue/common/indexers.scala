package com.blinkbox.books.catalogue.common

import com.sksamuel.elastic4s.{ElasticDsl => E, BulkDefinition, IndexDefinition}
import com.sksamuel.elastic4s.source.StringDocumentSource
import org.json4s._
import org.json4s.jackson.Serialization

object Indexers {
  implicit object BookIndexContent extends IndexContent[Book, EsIndexerTypes] {
    // TODO: Switch this to common-json provided formats
    implicit val formats = DefaultFormats

    override def single(content: Book): IndexDefinition =
      E.index into "catalogue/book" doc StringDocumentSource(Serialization.write(content))

    override def bulk(content: Iterable[Book]): BulkDefinition =
      E.bulk(content.map(single).toSeq: _*)
  }
}
