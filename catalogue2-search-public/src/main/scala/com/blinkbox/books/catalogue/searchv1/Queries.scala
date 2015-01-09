package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.ElasticsearchConfig
import com.blinkbox.books.catalogue.common.search.ElasticSearchSupport
import com.blinkbox.books.spray.Page
import com.blinkbox.books.spray.SortOrder
import com.sksamuel.elastic4s.ElasticDsl._

class Queries(searchConfig: ElasticsearchConfig) extends ElasticSearchSupport {

  private val DistributionStatusDocType = "distribution-status"

  private def searchIn(`type`: String) = search in s"${searchConfig.indexName}/${`type`}"

  override val SortFieldMapping = Map(
    "relevance" -> "_score",
    "author" -> "contributors.sortName",
    "popularity" -> "_score", // TODO - not yet implemented
    "price" -> "prices.amount",
    "publication_date" -> "dates.publish"
  )

  def mainSearch(q: String, page: Page, order: SortOrder) = paginate(page.offset, page.count) {
    sortBy(order.field, order.desc) {
      searchIn("book") query {
        filteredQuery query {
          dismax query (
            termQuery("isbn", q) boost 4,
            // Query for the title - give precedence to title that match including stop-words
            dismax query (
              matchPhrase("title", q) boost 1 slop 10,
              matchPhrase("titleSimple", q) boost 2 slop 10
            ) tieBreaker 0 boost 3, // No tie breaker as it would be pointless in this case
              nestedQuery("contributors") query (
                dismax query (
                  matchPhrase("contributors.displayName", q) slop 10 boost 10,
                  matches("contributors.displayName", q) operator "or" boost 5
                ) tieBreaker 0
              ) boost 2,
                  nestedQuery("descriptions") query (
                    matchPhrase("descriptions.content", q) slop 100
                  ) boost 1
          ) tieBreaker 0.2
        } filter {
          hasChildFilter(DistributionStatusDocType) filter termFilter("usable", true)
        }
      }
    }
  } suggestions (suggest using (phrase) as "spellcheck" on q from "spellcheck" size 1 maxErrors 3)

  def similarBooks(bookId: BookId, page: Page) = searchIn("book") query {
    similarBooksQuery(bookId.value)
  } filter {
    hasChildFilter(DistributionStatusDocType) filter termFilter("usable", true)
  } limit page.count from page.offset

  def suggestions(q: String, count: Int) = searchIn("catalogue") suggestions {
    suggest using (completion) as "autoComplete" on q from "autoComplete" size count
  } filter {
    hasChildFilter(DistributionStatusDocType) filter termFilter("usable", true)
  } limit 0 // We don't want search results, only suggestions
}
