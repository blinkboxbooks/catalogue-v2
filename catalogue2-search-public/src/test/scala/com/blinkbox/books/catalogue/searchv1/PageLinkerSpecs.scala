package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.spray.Page
import com.blinkbox.books.spray.v1.Link
import org.scalatest.{Matchers, FlatSpec}
import spray.http.Uri

class PageLinkerSpecs extends FlatSpec with Matchers {

  val uri = Uri("http://somehost.com/resource?param=foo&otherParam=bar&count=5")
  val query = uri.query.toMap

  def link(rel: String, uri: Uri, offset: String, count: String) =
    Link(rel, uri.withQuery(query.updated("count", count).updated("offset", offset)).toString, None, None)

  "The page linker" should "correctly build the 'this' link" in {
    PageLinker.links(uri, Page(0, 10), 10) should contain theSameElementsAs(
      link("this", uri, "0", "10") :: Nil)
  }

  it should "correctly build the 'next' link if possible" in {
    PageLinker.links(uri, Page(0, 5), 10) should contain theSameElementsAs(
      link("this", uri, "0", "5") ::
      link("next", uri, "5", "5") ::
      Nil)
  }

  it should "correctly build the 'prev' link if possible" in {
    PageLinker.links(uri, Page(5, 5), 10) should contain theSameElementsAs(
      link("this", uri, "5", "5") ::
      link("prev", uri, "0", "5") ::
      Nil)
  }

  it should "correctly build the all links if possible" in {
    PageLinker.links(uri, Page(10, 5), 20) should contain theSameElementsAs(
      link("this", uri, "10", "5") ::
      link("prev", uri, "5", "5") ::
      link("next", uri, "15", "5") ::
      Nil)
  }
}
