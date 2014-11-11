package com.blinkbox.books.catalogue.common

import com.sksamuel.elastic4s.{ElasticDsl => E}
import com.sksamuel.elastic4s.source.StringDocumentSource
import org.json4s._
import org.json4s.jackson.Serialization

class BookIndexContent extends IndexContent[Book, EsIndexer] {
  // TODO: Switch this to common-json provided formats
  implicit val formats = DefaultFormats

  override def single(content: Book): EsIndexer#IndexCommand =
    E.index into "catalogue/book" doc StringDocumentSource(Serialization.write(content))

  override def bulk(content: Iterable[Book]): EsIndexer#BulkIndexCommand =
    E.bulk(content.map(single).toSeq: _*)
}
