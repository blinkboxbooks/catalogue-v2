package com.blinkbox.books.catalogue.common.search

import org.joda.time.DateTime
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.RangeFilter
import org.elasticsearch.search.sort.SortOrder
import com.sksamuel.elastic4s.MoreLikeThisQueryDefinition

trait ElasticSearchSupport {
  def dateRangeFilter = rangeFilter("dates.publish")
      
  /** Adds an optional start/end date filter to the given search query. */
  def dateFilter(minDate: Option[DateTime], maxDate: Option[DateTime]): Option[RangeFilter] = {
    (minDate, maxDate) match {
      case (None, None) => Option.empty
      case (Some(start), None) => Some(dateRangeFilter.from(start))
      case (None, Some(end)) => Some(dateRangeFilter.to(end))
      case (Some(start), Some(end)) => Some(dateRangeFilter.from(start).to(end))
    }
  }
  
  /** Adds pagination filtering to the given query. */
  def paginate(offset: Int, count: Int)(query: SearchDefinition): SearchDefinition = query limit count from offset

  val SortFieldMapping: Map[String, String]

  /** Adds sorting and ordering to the given query. */
  def sortBy(field: String, descending: Boolean)(query: SearchDefinition) = {
    val sortField = SortFieldMapping.getOrElse(field.toLowerCase, throw new IllegalArgumentException(s"Invalid sort order: ${field}"))
    val sortOrder = if(descending) SortOrder.DESC else SortOrder.ASC

    query sort {
      by field sortField order sortOrder
    }
  }
  
  private def defaultMltField(field: String, isbn: String): MoreLikeThisQueryDefinition =
    morelikeThisQuery(field) minTermFreq 1 minDocFreq 1 minWordLength 3 maxQueryTerms 12 ids isbn

  /** Builds a more-like-this query. */
  def similarBooksQuery(isbn: String) = dismax query (
    defaultMltField("title", isbn),
    nestedQuery("descriptions") query(defaultMltField("descriptions.content", isbn)) boost 3,
    nestedQuery("contributors") query(defaultMltField("contributors.displayName", isbn)) boost 10,
    nestedQuery("subjects") query(defaultMltField("subjects.code", isbn)) boost 3
  )
}
