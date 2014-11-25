package com.blinkbox.books.catalogue.common.search

import com.blinkbox.books.catalogue.common.{IndexEntities => idx, SearchConfig, Book}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.source.DocumentSource
import org.elasticsearch.index.VersionType
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

class EsIndexer(config: SearchConfig, client: ElasticClient)(implicit ec: ExecutionContext) extends Indexer {
  import com.sksamuel.elastic4s.ElasticDsl.{index => esIndex, bulk}

  case class JsonSource(book: Book) extends DocumentSource {
    import com.blinkbox.books.catalogue.common.Json.formats
    def json = Serialization.write(idx.Book.fromMessage(book))
  }

  override def index(book: Book): Future[SingleResponse] = {
    client.execute {
      esIndex
        .into(s"${config.indexName}/book")
        .doc(JsonSource(book))
        .id(book.isbn)
        .versionType(VersionType.EXTERNAL)
        .version(book.sequenceNumber)
    } map { resp =>
      SingleResponse(resp.getId)
    }
  }

  override def index(books: Iterable[Book]): Future[Iterable[BulkItemResponse]] = {
    client.execute {
      bulk(
        books.map { book =>
          esIndex
            .into(s"${config.indexName}/book")
            .doc(JsonSource(book))
            .id(book.isbn)
            .versionType(VersionType.EXTERNAL)
            .version(book.sequenceNumber)
        }.toList: _*
      )
    } map { response =>
      response.getItems.map { item =>
        if(item.isFailed)
          Failure(item.getId, Some(new RuntimeException(item.getFailureMessage)))
        else
          Successful(item.getId)
      }
    }
  }
}

case class Schema(config: SearchConfig) {
  def classification = "classification" nested(
    "realm" typed StringType analyzer KeywordAnalyzer,
    "id" typed StringType analyzer KeywordAnalyzer
  )

  def uris = "uris" inner(
    "type" typed StringType analyzer KeywordAnalyzer,
    "uri" typed StringType index "not_analyzed",
    "params" typed StringType index "not_analyzed"
  )

  def availability(`type`: String) = `type` inner(
    "available" typed BooleanType,
    "code" typed StringType analyzer KeywordAnalyzer,
    "extra" typed StringType
  )

  def otherText(name: String) = name nested(
    classification,
    "content" typed StringType analyzer "descriptionAnalyzer",
    "type" typed StringType analyzer KeywordAnalyzer,
    "author" typed StringType analyzer WhitespaceAnalyzer
  )

  def regions(name: String) = name nested(
    "GB" typed BooleanType nullValue false,
    "ROW" typed BooleanType nullValue false,
    "WORLD" typed BooleanType nullValue false
  )

  def catalogue = (create index config.indexName mappings (
    "book" as(
      "sequenceNumber" typed LongType,
      classification,
      "isbn" typed StringType analyzer KeywordAnalyzer,
      "format" inner(
        "marvinIncompatible" typed BooleanType,
        "epubType" typed StringType,
        "productForm" typed StringType
      ),
      "title" typed StringType analyzer SnowballAnalyzer,
      "subtitle" typed StringType analyzer SnowballAnalyzer,
      "contributors" nested (
        "role" typed StringType analyzer KeywordAnalyzer,
        "id" typed StringType analyzer KeywordAnalyzer,
        "displayName" typed StringType analyzer SimpleAnalyzer,
        "sortName" typed StringType analyzer KeywordAnalyzer
        ),
      "availability" inner(
        availability("notificationType"),
        availability("publishingStatus"),
        availability("availabilityCode"),
        availability("productAvailability"),
        availability("blinkboxBooks")
      ),
      "dates" inner(
        "publish" typed DateType,
        "announce" typed DateType
      ),
      otherText("descriptions"),
      otherText("reviews"),
      "languages" typed StringType analyzer KeywordAnalyzer,
      "originalLanguages" typed StringType analyzer KeywordAnalyzer,
      regions("supplyRights"),
      regions("salesRights"),
      "publisher" typed StringType analyzer KeywordAnalyzer,
      "imprint" typed StringType,
      "prices" nested(
        "amount" typed DoubleType,
        "currency" typed StringType analyzer KeywordAnalyzer,
        "includeTax" typed BooleanType,
        "isAgency" typed BooleanType,
        "discountRate" typed IntegerType,
        "validFrom" typed DateType,
        "validUntil" typed DateType,
        regions("applicableRegions"),
        "tax" nested(
          "rate" typed StringType,
          "percent" typed DoubleType,
          "amount" typed DoubleType,
          "taxableAmount" typed DoubleType
        )
      ),
      "statistics" inner(
        "pages" typed IntegerType,
        "sentences" typed IntegerType,
        "words" typed IntegerType,
        "syllables" typed IntegerType,
        "polysyllables" typed IntegerType,
        "smog_grade" typed IntegerType, // TODO: Change to use camel case
        "adultThemes" typed BooleanType
      ),
      "subjects" nested(
        "type" typed StringType analyzer KeywordAnalyzer,
        "code" typed StringType analyzer KeywordAnalyzer,
        "main" typed BooleanType
      ),
      "series" nested(
        "title" typed StringType,
        "number" typed IntegerType
      ),
      "related" nested(
        classification,
        "relation" typed StringType analyzer KeywordAnalyzer,
        "isbn" typed StringType analyzer KeywordAnalyzer
      ),
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
      "distributionStatus" inner(
        "usable" typed BooleanType,
        "reasons" typed StringType
      ),
      "source" inner(
        "deliveredAt" typed DateType,
        "uri" typed StringType,
        "fileName" typed StringType,
        "contentType" typed StringType,
        "role" typed StringType,
        "username" typed StringType,
        "system" inner(
          "name" typed StringType,
          "version" typed StringType
        ),
        "processedAt" typed DateType
      ),
      // Calculated fields for specific search scenarios
      "descriptionContents" typed StringType,
      "autoComplete" typed CompletionType payloads(true)
    ) dynamic(false)
  )).analysis(
    CustomAnalyzerDefinition("descriptionAnalyzer",
      StandardTokenizer,
      HtmlStripCharFilter,
      StandardTokenFilter,
      LowercaseTokenFilter,
      StopTokenFilter("descriptionStopWords"),
      SnowballTokenFilter("descriptionSnowball")
  ))
}
