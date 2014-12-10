package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.spray.Page
import com.blinkbox.books.spray.v1.Link
import org.scalatest.{Matchers, FlatSpec}
import spray.http.Uri

class PageLinkerSpecs extends FlatSpec with Matchers {

  val uri = Uri("http://somehost.com/resource?param=foo&otherParam=bar&count=5")
  val query = uri.query.toMap

  def link(rel: String, uri: Uri) = Link(rel, uri.toString, None, None)

  "The page linker" should "correctly build the 'this' link" in {
    PageLinker.links(uri, Page(0, 10), 10) should equal(Seq(link("this", uri)))
  }

  it should "correctly build the 'next' link if possible" in {
    PageLinker.links(uri, Page(0, 5), 10) should contain theSameElementsAs(
      link("this", uri) :: link("next", uri.withQuery(query.updated("offset", "5").updated("count", "5"))) :: Nil)
  }

  it should "correctly build the 'prev' link if possible" in {
    PageLinker.links(uri, Page(5, 5), 10) should contain theSameElementsAs(
      link("this", uri) :: link("prev", uri.withQuery(query.updated("offset", "0").updated("count", "5"))) :: Nil)
  }

  it should "correctly build the all links if possible" in {
    PageLinker.links(uri, Page(10, 5), 20) should contain theSameElementsAs(
      link("this", uri) ::
      link("prev", uri.withQuery(query.updated("offset", "5").updated("count", "5"))) ::
      link("next", uri.withQuery(query.updated("offset", "15").updated("count", "5"))) ::
      Nil)
  }
}
