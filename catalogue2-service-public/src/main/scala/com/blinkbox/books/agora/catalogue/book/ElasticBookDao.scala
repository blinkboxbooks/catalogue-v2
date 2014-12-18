package com.blinkbox.books.agora.catalogue.book

import java.util.concurrent.Executors
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.logging.DiagnosticExecutionContext
import scala.concurrent.{ExecutionContext, Future}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.json4s.jackson.Serialization
import com.blinkbox.books.json.DefaultFormats
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.get.GetResponse
import org.joda.time.DateTime
import org.elasticsearch.search.sort.SortOrder
import com.blinkbox.books.catalogue.common.search.ElasticSearchSupport

class ElasticBookDao(client: ElasticClient, index: String) extends BookDao with ElasticSearchSupport {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))
  implicit val formats = DefaultFormats
  
  override val SortFieldMapping = Map(
    "title" -> "titleSimple",
    "sales_rank" -> "titleSimple", // TODO - not yet implemented
    "publication_date" -> "dates.publish",
    "price" -> "prices.amount",
    "author" -> "contributors.sortName",
    "sequential" -> "_score" // TODO - PK of category/promotion
  )

  private def toBook(book: String): Book = Serialization.read[Book](book)

  private def toBook(res: GetResponse): Book = toBook(res.getSourceAsString)

  private def toBookList(res: SearchResponse): BookList = {
    val hits = res.getHits()
    BookList(hits.hits.toList.map(hit => toBook(hit.getSourceAsString)), hits.getTotalHits().toInt)
  }

  override def getBookByIsbn(isbn: String): Future[Option[Book]] = {
    client.execute {
      get id isbn from index
    } map { res =>
      if(res.isSourceEmpty) None else Some(toBook(res))
    }
  }
  
  override def getBooks(isbns: List[String]): Future[List[Book]] = {
    client.execute {
      multiget(isbns.map(isbn => get id isbn from index).toSeq: _*)
    } map { multiRes => multiRes.getResponses().toList
      .filter(item => !item.getResponse().isSourceEmpty())
      .map(item => toBook(item.getResponse()))
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
      }
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
      }
    } map toBookList
  }
}
