package com.blinkbox.books.agora.catalogue.book

import java.util.concurrent.Executors
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.logging.DiagnosticExecutionContext
import scala.concurrent.{ExecutionContext, Future}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.json4s.jackson.Serialization
import com.blinkbox.books.json.DefaultFormats
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.get.GetResponse
import com.sksamuel.elastic4s.RangeFilter
import org.joda.time.DateTime

class ElasticBookDao(client: ElasticClient, index: String) extends BookDao {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))
  implicit val formats = DefaultFormats
  
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
  
  private def mapSortOrder(descending: Boolean): SortOrder = if(descending) SortOrder.DESC else SortOrder.ASC
  
  private val sortFieldMapping = Map(
    "title" -> "title",
    "sales_rank" -> "title", 						// TODO - not yet implemented
    "publication_date" -> "dates.publish",
    "price" -> "prices.amount",
    "sequential" -> "_score",
    "author" -> "contributors.sortName"
  )

  private def mapSortField(field: String): String = sortFieldMapping.getOrElse(field, throw new IllegalArgumentException(s"Invalid sort order: ${field}"))

  private def dateFilter(minDate: Option[DateTime], maxDate: Option[DateTime]): RangeFilter = {
    val range = rangeFilter("dates.publish")
    (minDate, maxDate) match {
      case (None, None) => null
      case (Some(start), None) => range.from(minDate)
      case (None, Some(end)) => range.to(end)
      case (Some(start), Some(end)) => range.from(start).to(end)
    }
  }

  override def getBooksByContributor(id: String, minDate: Option[DateTime], maxDate: Option[DateTime], offset: Int, count: Int, sortField: String, sortDescending: Boolean): Future[BookList] = {
    require(offset >= 0, "Offset must be zero-or-more")
    require(count > 0, "Count must be one-or-more")

    val query = search in index query {
      nestedQuery("contributors") query {
        termQuery("contributors.id", id)
      }
    } limit count from offset sort {
      by field mapSortField(sortField) order mapSortOrder(sortDescending)
    }
      
    val f = dateFilter(minDate, maxDate)
    
    client.execute {
      if(f == null) query else query.filter(f)
    } map toBookList
  }

  override def getRelatedBooks(isbn: String, offset: Int, count: Int): Future[BookList] = {
    require(offset >= 0, "Offset must be zero-or-more")
    require(count > 0, "Count must be one-or-more")
    
    client.execute {
      search in index query {
        morelikeThisQuery("title", "descriptionContents") minTermFreq 1 minDocFreq 1 minWordLength 3 maxQueryTerms 12 ids isbn
      } limit count from offset
    } map toBookList
  }
}
