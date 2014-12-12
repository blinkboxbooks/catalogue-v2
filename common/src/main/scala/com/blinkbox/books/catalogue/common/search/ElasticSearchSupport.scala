package com.blinkbox.books.catalogue.common.search

import org.joda.time.DateTime
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

trait ElasticSearchSupport {
  /**
   * Adds an optional start/end date filter to the given search query.
   */
  def dateFilter(minDate: Option[DateTime], maxDate: Option[DateTime])(query: SearchDefinition): SearchDefinition = {
    def filter = rangeFilter("dates.publish")
    val dateFilter = (minDate, maxDate) match {
      case (None, None)             => Option.empty
      case (Some(start), None)      => Some(filter.from(start))
      case (None, Some(end))        => Some(filter.to(end))
      case (Some(start), Some(end)) => Some(filter.from(start).to(end))
    }
    dateFilter.map(f => query.filter(f)).getOrElse(query)
  }
  
  /**
   * Adds pagination filtering to the given query.
   */
  def paginate(offset: Int, count: Int)(query: SearchDefinition): SearchDefinition = {
    query limit count from offset
  }

  val SortFieldMapping = Map(
    "title" ->              "title",
    "sales_rank" ->         "title", 						// TODO - not yet implemented
    "publication_date" ->   "dates.publish",
    "price" ->              "prices.amount",
    "sequential" ->         "_score",
    "author" ->             "contributors.sortName"
  )

  /**
   * Adds sorting and ordering to the given query.
   */
  def sortBy(field: String, descending: Boolean)(query: SearchDefinition) = {
    val sortField = SortFieldMapping.getOrElse(field, throw new IllegalArgumentException(s"Invalid sort order: ${field}"))
    val sortOrder = if(descending) SortOrder.DESC else SortOrder.ASC
    query sort {
      by field sortField order sortOrder
    }
  }
}
