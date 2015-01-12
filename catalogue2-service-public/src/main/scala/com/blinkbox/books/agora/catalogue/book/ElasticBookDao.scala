package com.blinkbox.books.agora.catalogue.book

import java.util.concurrent.Executors
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common.Json
import com.blinkbox.books.catalogue.common.search.ElasticSearchSupport
import com.blinkbox.books.elasticsearch.client.{SearchResponse, UnsuccessfulResponse, ElasticClientApi, ElasticClient}
import com.blinkbox.books.json.DefaultFormats
import com.blinkbox.books.logging.DiagnosticExecutionContext
import org.json4s.JsonAST.JValue
import spray.http.StatusCodes
import com.sksamuel.elastic4s.ElasticDsl._
import org.joda.time.DateTime
import scala.concurrent.{Future, ExecutionContext}

class ElasticBookDao(client: ElasticClient, index: String) extends BookDao with ElasticSearchSupport {
  import ElasticClientApi._
  import Json._

  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))
  implicit val formats = DefaultFormats

  override val SortFieldMapping = Map(
    "title" -> "title.titleSort",
    "sales_rank" -> "title.titleSort", // TODO - not yet implemented
    "publication_date" -> "dates.publish",
    "price" -> "prices.amount",
    "author" -> "contributors.sortName",
    "sequential" -> "_score" // TODO - PK of category/promotion
  )

  private def toBookList(res: SearchResponse[Book, JValue]): BookList = {
    val hits = res.hits
    BookList(hits.hits.toList.map(hit => hit._source), hits.total)
  }

  override def getBookByIsbn(isbn: String): Future[Option[Book]] = {
    client.execute {
      (get id isbn from index).sourceIs[Book]
    } map(_._source) recover {
      case UnsuccessfulResponse(StatusCodes.NotFound, _) => None
    }
  }

  override def getBooks(isbns: List[String]): Future[List[Book]] = {
    client.execute {
      multiget(isbns.map(isbn => get id isbn from index).toSeq: _*).sourceIs[Book]
    } map { multiRes =>
      multiRes.docs.toList.flatMap(_._source)
    }
  }

  override def getBooksByContributor(id: String, minDate: Option[DateTime], maxDate: Option[DateTime], offset: Int, count: Int, sortField: String, sortDescending: Boolean): Future[BookList] = {
    require(offset >= 0, "Offset must be zero-or-more")
    require(count > 0, "Count must be one-or-more")

    client.execute {
      paginate(offset, count) {
        sortBy(sortField, sortDescending) {
          search in index query {
            nestedQuery("contributors") query {
              termQuery("contributors.id", id)
            }
          } filter dateFilter(minDate, maxDate).getOrElse(matchAllFilter)
        }
      }.sourceIs[Book]
    } map toBookList
  }

  override def getRelatedBooks(isbn: String, offset: Int, count: Int): Future[BookList] = {
    require(offset >= 0, "Offset must be zero-or-more")
    require(count > 0, "Count must be one-or-more")

    client.execute {
      paginate(offset, count) {
        search in index query {
          similarBooksQuery(isbn)
        }
      }.sourceIs[Book]
    } map toBookList
  }
}
