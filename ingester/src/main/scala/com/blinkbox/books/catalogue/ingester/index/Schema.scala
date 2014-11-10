package com.blinkbox.books.catalogue.ingester.index

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{WhitespaceAnalyzer, KeywordAnalyzer, SnowballAnalyzer}
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, BooleanType, ObjectType, StringType}

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
