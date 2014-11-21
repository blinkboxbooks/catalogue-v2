package com.blinkbox.books.catalogue.ingester.v2.parser

import com.blinkbox.books.catalogue.common._
import com.blinkbox.books.messaging.{MediaType, EventBody, JsonEventBody}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.json4s.jackson.Serialization
import scala.util.{Success, Failure, Try}
import scala.xml.{NodeSeq, XML, Elem}

trait IngestionParser[T, R] {
  def parse(content: T): Try[R]
}

class XmlV1IngestionParser extends IngestionParser[String, DistributeContent]{
  private val ModifiedAtFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z")
  private val PublishedOnFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  private val AnnouncedOnFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  override def parse(content: String): Try[DistributeContent] =
    Try(XML.loadString(content)).map { xml =>
      xml.label match {
        case "book" => xmlToBook(xml)
        case "undistribute" => Undistribute(
          isbn = (xml \\ "undistribute" \ "book").text,
          effectiveTimestamp = (xml \\ "undistribute" \ "effectiveTimestamp")
            .text.opt[DateTime]
            .getOrElse(throw new IllegalArgumentException("Missing 'effectiveTimestamp' field.")))
        case _ => throw new IllegalArgumentException(s"Unexpected XML with root element: ${xml.label}")
      }
    }

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
      publisher = (xmlBook \ "publisher" \ "name").text.opt[String],
      prices = toPrices(xmlBook),
      subjects = toSubjects(xmlBook),
      supplyRights = toRegionalRights(xmlBook),
      media = toMedia(xmlBook),
      sequenceNumber = toModifiedAt(xmlBook).getMillis,
      dates = toDates(xmlBook))
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

  private def toDescriptions(xml: NodeSeq, contributors: List[Contributor]): List[OtherText] = {
    val author = contributors.map(_.role).find(_ == "Author").getOrElse("NONE")
    (xml \ "descriptions").map { node =>
      val description = node \ "description"
      val classification = List(
        Classification(realm = "format", id = (description \ "@format").text),
        Classification(realm = "source", id = (description \ "@source").text))
      OtherText(
        classification = classification,
        content = description.text,
        `type` = (description \ "@format").text,
        author = author.opt[String])
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
        validFrom = Option.empty[DateTime],
        validUntil = Option.empty[DateTime],
        applicableRegions = Option.empty[Regions],
        tax = (node \ "tax").text.opt[Double].map(value => Tax("H", None, Some(value), None)))
    }.toList

  private def toSubjects(xml: NodeSeq): List[Subject] =
    (xml \ "subjects").map { node =>
      Subject(
        `type` = (node \ "@type").toString,
        code = node.text,
        main = None)
    }.toList

  private def toRegionalRights(xml: NodeSeq): Option[Regions] = {
    val regions = (xml \ "regions").map(node => (node \ "region").text.toUpperCase)
    val emptyRegionalRights = Regions(None, None, None)
    Some(regions
      .foldLeft(emptyRegionalRights)((acc, element) =>
          element match {
            case "GB" => acc.copy(`GB` = Some(true))
            case "WORLD" => acc.copy(`WORLD` = Some(true))
            case "ROW" => acc.copy(`ROW` = Some(true))
            case _ => acc
          }
      ))
  }

  private def toMedia(xml: NodeSeq): Option[Media] = {
    val coverNode = xml \ "media" \ "cover"
    val epubNodes = xml \ "media" \ "epub"
    val coverClassification = Classification(realm = "type", id = "front_cover")
    val epubs = epubNodes.map { node =>
      Epub(
        classification = List(Classification(realm = "type", id = (node \ "@type").toString)),
        uris = List(Uri(`type` = "static", uri = node.text, params = None)),
        keyFile = None,
        wordCount = 0,
        size = (node \ "@size").toString.opt[Long].getOrElse(0))
    }.toList
    val images = List(
      Image(
        classification = List(coverClassification),
        uris = List(Uri(`type` = "static", uri = coverNode.text, params = None)),
        width = 0,
        height = 0,
        size = (coverNode \ "@size").toString.opt[Int].getOrElse(0)))
    Some(Media(
      epubs = epubs,
      images = images))
  }

  private def toModifiedAt(xml: NodeSeq): DateTime = {
    val modifiedAt = (xml \ "modifiedAt").text
    if(modifiedAt.isEmpty) throw new RuntimeException("Missing 'modifiedAt' field.")
    else ModifiedAtFormatter.parseDateTime(modifiedAt)
  }

  private def toDates(xml: NodeSeq): Option[Dates] = {
    val publishDate = (xml \ "publishedOn").text
    val announceDate = (xml \ "announcedOn").text
    Some(Dates(
      publish =
        if(publishDate.isEmpty) None
        else Some(PublishedOnFormatter.parseDateTime(publishDate)),
      announce =
        if(announceDate.isEmpty) None
        else Some(AnnouncedOnFormatter.parseDateTime(announceDate))
    ))
  }
}

class JsonV2IngestionParser extends IngestionParser[EventBody,Book] {
//  implicit object Book extends JsonEventBody[Book] {
//    val jsonMediaType = MediaType("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json")
//  }

  import Json.formats

  override def parse(content: EventBody): Try[Book] =
    Try(Serialization.read[Book](new String(content.content, "UTF-8")))
      .transform(
        book => Success(book),
        e => Failure(new RuntimeException("Not able to parse json", e)))
      //.recover({case e: Exception => throw new RuntimeException("Not able to parse json", e)})
//    Try(JsonEventBody.unapply[Book](content).getOrElse(throw new RuntimeException("Not able to parse json")))
//      .orElse(Failure(new RuntimeException("Invalid json format")))
}
