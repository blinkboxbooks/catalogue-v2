package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.spray.Page
import com.blinkbox.books.spray.v1.{Link, Version1JsonSupport}
import org.json4s._
import spray.http.Uri

object PageLinker {
  sealed trait HRef
  case object Prev extends HRef
  case object This extends HRef
  case object Next extends HRef

  private def updateOffset(uri: Uri, count: Int, offset: Int) =
    uri.withQuery(uri.query.toMap.updated("count", count.toString).updated("offset", offset.toString))

  private def someLink(href: String, uri: Uri, count: Int, offset: Int): Option[Link] = Some(Link(href, updateOffset(uri, count, offset).toString, None, None))

  private def buildLink(uri: Uri, page: Page, numberOfResults: Long)(href: HRef): Option[Link] = href match {
    case Prev => if (page.offset == 0) None else someLink("prev", uri, page.count, page.offset - page.count)
    case This => someLink("this", uri.toString, page.count, page.offset)
    case Next => if (page.offset + page.count >= numberOfResults) None else someLink("next", uri, page.count, page.offset + page.count)
  }

  def links(uri: Uri, page: Page, numberOfResults: Long): Seq[Link] =
    Seq(Prev, This, Next).map(buildLink(uri, page, numberOfResults)).flatten
}

class PagedSerializer extends Serializer[Paged[_ <: AnyRef]] {
  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Paged[_ <: AnyRef]] =
    sys.error("Cannot deserialize Paged[_] instances")

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: Paged[_] =>
      JObject("links" -> Extraction.decompose(PageLinker.links(x.uri, x.page, x.numberOfResults))).merge(
        Extraction.decompose(x.content))
  }
}

trait Serialization extends Version1JsonSupport {
  override implicit def version1JsonFormats: Formats = super.version1JsonFormats + new PagedSerializer
}
