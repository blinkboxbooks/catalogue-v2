package com.blinkbox.books.catalogue.common.search

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.joda.time.DateTime
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

@RunWith(classOf[JUnitRunner])
class ElasticSearchSupportTest extends FlatSpec {
  val start = Some(new DateTime(1))
  val end = Some(new DateTime(2))
  
  class QueryFixture extends ElasticSearchSupport {
    val query = new SearchDefinition("field")
    val filter = rangeFilter("dates.publish")
  }

  it should "not apply a filter if no date-range parameters are provided" in new QueryFixture {
    assert(query == dateFilter(None, None)(query))
  }

  it should "filter by start date" in new QueryFixture {
    val result = dateFilter(start, None)(query)
    val expected = query.filter(filter.from(start))
    assert(result == expected)
  }

  it should "filter by end date" in new QueryFixture {
    val result = dateFilter(start, end)(query)
    val expected = query.filter(filter.from(start).to(end))
    assert(result == expected)
  }

  it should "filter by both start and end dates" in new QueryFixture {
    val result = dateFilter(start, None)(query)
    val expected = query.filter(filter.to(end))
    assert(result == expected)
  }
  
  it should "apply pagination" in new QueryFixture {
    val result = paginate(1, 2)(query)
    val expected = query limit 1 from 2
    assert(result == expected)
  }

  it should "apply sorting" in new QueryFixture {
    val result = sortBy("title", true)(query)
    val expected = query sort { by field "title" order SortOrder.DESC }
    assert(result == expected)
  }
  
  it should "fail on an invalid sort-order" in new QueryFixture {
    intercept[IllegalArgumentException] {
      sortBy("cobblers", true)(query)
    }
  }
}
