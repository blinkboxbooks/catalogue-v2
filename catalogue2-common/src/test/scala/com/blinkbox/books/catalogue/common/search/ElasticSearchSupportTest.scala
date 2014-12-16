package com.blinkbox.books.catalogue.common.search

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.joda.time.DateTime
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

@RunWith(classOf[JUnitRunner])
class ElasticSearchSupportTest extends FlatSpec {
  val start = new DateTime(1)
  val end = new DateTime(2)
  
  class QueryFixture extends ElasticSearchSupport {
    val query = new SearchDefinition("field")
    override val dateRangeFilter = rangeFilter("dates.publish")
  }

  "The ES support helper" should "not apply a filter if no date-range parameters are provided" in new QueryFixture {
    assert(None == dateFilter(None, None))
  }

  it should "filter by start date" in new QueryFixture {
    val result = dateFilter(Some(start), None)
    assert(result == Some(dateRangeFilter.from(start)))
  }

  it should "filter by end date" in new QueryFixture {
    val result = dateFilter(None, Some(end))
    assert(result == Some(dateRangeFilter.to(end)))
  }

  it should "filter by both start and end dates" in new QueryFixture {
    val result = dateFilter(Some(start), Some(end))
    assert(result == Some(dateRangeFilter.from(start).to(end)))
  }
  
  it should "apply pagination" in new QueryFixture {
    val result = paginate(1, 2)(query)
    val expected = query limit 1 from 2
    assert(result == expected)
  }

  it should "apply sorting" in new QueryFixture {
    val result = sortBy("title", true)(query)
    val expected = query sort { by field "titleSimple" order SortOrder.DESC }
    assert(result == expected)
  }
  
  it should "fail on an invalid sort-order" in new QueryFixture {
    intercept[IllegalArgumentException] {
      sortBy("cobblers", true)(query)
    }
  }
  
  it should "build a similar books query" in new QueryFixture {
    val similarQuery = similarBooksQuery("isbn")
    val json = similarQuery.builder.toString
    assert(json contains "title")
    assert(json contains "descriptions.content")
    assert(json contains "contributors.displayName")
    assert(json contains "subjects.code")
  }
}
