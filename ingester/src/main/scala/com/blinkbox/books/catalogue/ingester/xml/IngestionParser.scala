package com.blinkbox.books.catalogue.ingester.xml

import com.blinkbox.books.catalogue.common._
import com.blinkbox.books.messaging.{MediaType, EventBody, JsonEventBody}
import scala.util.{Failure, Try}
import scala.xml.{NodeSeq, XML, Elem}

trait IngestionParser[T, R] {
  def parse(content: T): Try[R]
}

class XmlV1IngestionParser extends IngestionParser[String, Book]{
  override def parse(content: String): Try[Book] =
    Try(xmlToBook(XML.loadString(content)))

  private def xmlToBook(xml: Elem): Book = {
    val xmlBook = xml \\ "book"
    val contributors = toContributors(xmlBook)
    Book.empty.copy(
      isbn = (xmlBook \ "isbn").text,
      title = (xmlBook \ "title").text,
      subtitle = (xmlBook \ "subtitle").text.opt[String],
      series = toSeries(xmlBook),
      contributors = contributors,
      descriptions = toDescriptions(xmlBook, contributors),
      publisher = (xmlBook \ "publisher" \ "name").text,
      prices = toPrices(xmlBook),
      subjects = toSubjects(xmlBook),
      regionalRights = toRegionalRights(xmlBook),
      media = toMedia(xmlBook))
  }

  private def toContributors(xml: NodeSeq): List[Contributor] =
    (xml \ "contributors").map{ node =>
      val contributor = node \ "contributor"
      Contributor(
        (contributor \ "@role").toString,
        (contributor \ "@id").toString,
        (contributor \ "displayName").text,
        (contributor \ "sortName").text)
    }.toList

  private def toSeries(xml: NodeSeq): Option[Series] =
    Some(Series(
      title = (xml \ "series" \ "title").text,
      number = (xml \ "series" \ "number").text.opt[Int]))

  private def toDescriptions(xml: NodeSeq, contributors: List[Contributor]): List[Description] = {
    val author = contributors.map(_.role).find(_ == "Author").getOrElse("NONE")
    (xml \ "descriptions").map { node =>
      val description = node \ "description"
      val classification = List(
        Classification(realm = "format", id = (description \ "@format").text),
        Classification(realm = "source", id = (description \ "@source").text))
      Description(
        classification = classification,
        content = description.text,
        `type` = (description \ "@format").text,
        author = author)
    }.toList
  }

  private def toPrices(xml: NodeSeq): List[Price] =
    (xml \ "prices").map { node =>
      Price(
        amount = (node \ "amount").text.opt[Double].getOrElse(0.0),
        currency = (node \ "@currency").toString,
        includesTax = (node \ "tax" \ "@included").toString.opt[Boolean].getOrElse(false),
        isAgency = (node \ "@agency").toString.opt[Boolean].getOrElse(false),
        discountRate = Option.empty[Int],
        tax = (node \ "tax").text.opt[Double].map(Tax))
    }.toList

  private def toSubjects(xml: NodeSeq): List[Subject] =
    (xml \ "subjects").map { node =>
      Subject(
        `type` = (node \ "@type").toString,
        code = node.text)
    }.toList

  private def toRegionalRights(xml: NodeSeq): RegionalRights = {
    val regions = (xml \ "regions").map(node => (node \ "region").text.toUpperCase)
    val emptyRegionalRights = RegionalRights(None, None, None)
    regions
      .foldLeft(emptyRegionalRights)((acc, element) =>
          element match {
            case "GB" => acc.copy(`GB` = Some(true))
            case "WORLD" => acc.copy(`WORLD` = Some(true))
            case "ROW" => acc.copy(`ROW` = Some(true))
            case _ => acc
          }
      )
  }

  private def toMedia(xml: NodeSeq): Media = {
    val coverNode = xml \ "media" \ "cover"
    val epubNodes = xml \ "media" \ "epub"
    val coverClassification = Classification(realm = "type", id = "front_cover")
    val epubs = epubNodes.map { node =>
      Epub(
        classification = List(Classification(realm = "type", id = (node \ "@type").toString)),
        uris = List(Uri(`type` = "static", uri = node.text, params = None)),
        keyFile = None,
        wordCount = 0,
        size = (node \ "@size").toString.toInt)
    }.toList
    val images = List(
      Image(
        classification = List(coverClassification),
        uris = List(Uri(`type` = "static", uri = coverNode.text, params = None)),
        width = 0,
        height = 0,
        size = (coverNode \ "@size").toString.toInt))
    Media(
      epubs = epubs,
      images = images)
  }
}

class JsonV2IngestionParser extends IngestionParser[EventBody,Book] {
  implicit object Book extends JsonEventBody[Book] {
    val jsonMediaType = MediaType("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json")
  }

  override def parse(content: EventBody): Try[Book] =
    Try(JsonEventBody.unapply[Book](content).getOrElse(throw new RuntimeException("Not able to parse json")))
      .orElse(Failure(new RuntimeException("Invalid json format")))
}