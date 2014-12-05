package com.blinkbox.books.catalogue.ingester.v1.parser

import com.blinkbox.books.catalogue.common.Events.{BookPrice, Undistribute, Book}
import com.blinkbox.books.catalogue.common._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Try
import scala.xml.{NodeSeq, XML, Elem}

trait IngestionParser[T, R] {
  def parse(content: T): Try[R]
}

case class InvalidContentException(content: String) extends RuntimeException(content)
case class MissingFieldException(field: String) extends RuntimeException(field)

trait XmlV1IngestionParser extends IngestionParser[String, DistributeContent]{
  override def parse(content: String): Try[DistributeContent] =
    Try(XML.loadString(content))
      .recover({case _ => throw InvalidContentException("Invalid xml document.")})
      .map(xml => produceDistributeContent(xml)(xml.label))

  def produceDistributeContent(xml: Elem): PartialFunction[String, DistributeContent]

  implicit class RichNodeSeq(nodeSeq: NodeSeq) {
    def trimText = nodeSeq.text.trim
  }
}

class BookXmlV1IngestionParser extends XmlV1IngestionParser {
  private val ModifiedAtFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z")
  private val PublishedOnFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  private val AnnouncedOnFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  override def produceDistributeContent(xml: Elem): PartialFunction[String, DistributeContent] = {
    case "book" => toBook(xml)
    case "undistribute" => toUndistribute(xml)
    case _ => throw InvalidContentException(s"${xml.label}")
  }

  def toBook(xml: Elem): Book = {
    val xmlBook = xml \\ "book"
    val contributors = toContributors(xmlBook)
    Book.empty.copy(
      sequenceNumber = toModifiedAt(xmlBook).getMillis,
      isbn = (xmlBook \ "isbn").trimText,
      title = (xmlBook \ "title").trimText,
      subtitle = (xmlBook \ "subtitle").trimText.opt[String],
      series = toSeries(xmlBook),
      contributors = contributors,
      descriptions = toDescriptions(xmlBook, contributors),
      publisher = (xmlBook \ "publisher" \ "name").trimText.opt[String],
      prices = toPrices(xmlBook),
      subjects = toSubjects(xmlBook),
      languages = List((xmlBook \ "language").trimText),
      supplyRights = Option(toRegionalRights(xmlBook)),
      dates = toDates(xmlBook),
      media = Option(toMedia(xmlBook)),
      source = toSource(xmlBook)
    )
  }

  def toUndistribute(xml: Elem): Undistribute = {
    val undistributeXml = xml \\ "undistribute"
    Undistribute(
      isbn = (undistributeXml \ "book").trimText,
      sequenceNumber = (undistributeXml \ "effectiveTimestamp")
        .trimText.opt[DateTime]
        .getOrElse(throw MissingFieldException("effectiveTimestamp"))
        .getMillis,
      reasons = toReasons(undistributeXml)
    )
  }

  private def toContributors(xml: NodeSeq): List[Contributor] =
    (xml \ "contributors" \ "contributor").map{ node =>
      Contributor(
        (node \ "@role").toString,
        (node \ "@id").toString,
        (node \ "displayName").trimText,
        (node \ "sortName").trimText)
    }.toList

  private def toSeries(xml: NodeSeq): Option[Series] =
    Option(
      Series(
        title = (xml \ "series" \ "title").trimText,
        number = (xml \ "series" \ "number").trimText.opt[Int]
      )
    )

  private def toDescriptions(xml: NodeSeq, contributors: List[Contributor]): List[OtherText] = {
    val author = contributors.map(_.role).find(_ == "Author").getOrElse("NONE")
    (xml \ "descriptions" \ "description").map { node =>
      val classification = List(
        Classification(realm = "format", id = (node \ "@format").toString),
        Classification(realm = "source", id = (node \ "@source").toString))
      OtherText(
        classification = classification,
        content = node.trimText,
        `type` = (node \ "@format").toString,
        author = author.opt[String])
    }.toList
  }

  private def toPrices(xml: NodeSeq): List[Price] =
    (xml \ "prices" \ "price").map { node =>
      Price(
        amount = (node \ "amount").trimText.opt[Double].getOrElse(0.0),
        currency = (node \ "@currency").toString,
        includesTax = (node \ "tax" \ "@included").toString.opt[Boolean].getOrElse(false),
        isAgency = (node \ "@agency").toString.opt[Boolean].getOrElse(false),
        discountRate = Option.empty[Int],
        validFrom = Option.empty[DateTime],
        validUntil = Option.empty[DateTime],
        applicableRegions = Option.empty[Regions],
        tax = (node \ "tax").trimText.opt[Double].map(value => Tax("H", None, Some(value), None)))
    }.toList

  private def toSubjects(xml: NodeSeq): List[Subject] =
    (xml \ "subjects" \ "subject").map { node =>
      Subject(
        `type` = (node \ "@type").toString,
        code = node.trimText,
        main = None)
    }.toList

  private def toRegionalRights(xml: NodeSeq): Regions = {
    val regions = (xml \ "regions" \ "region").map(node => node.trimText.toUpperCase)
    val emptyRegionalRights = Regions(None, None, None)
    regions.foldLeft(emptyRegionalRights)((acc, element) =>
      element match {
        case "GB" => acc.copy(`GB` = Some(true))
        case "WORLD" => acc.copy(`WORLD` = Some(true))
        case "ROW" => acc.copy(`ROW` = Some(true))
        case _ => acc
      })
  }

  private def toMedia(xml: NodeSeq): Media = {
    val coverNode = xml \ "media" \ "cover"
    val epubNodes = xml \ "media" \ "epub"
    val coverClassification = Classification(realm = "type", id = "front_cover")
    val epubs = epubNodes.map { node =>
      Epub(
        classification = List(Classification(realm = "type", id = (node \ "@type").toString)),
        uris = List(Uri(`type` = "static", uri = node.trimText, params = None)),
        keyFile = None,
        wordCount = 0,
        size = (node \ "@size").toString.opt[Long].getOrElse(0))
    }.toList
    val images = List(
      Image(
        classification = List(coverClassification),
        uris = List(Uri(`type` = "static", uri = coverNode.trimText, params = None)),
        width = 0,
        height = 0,
        size = (coverNode \ "@size").toString.opt[Int].getOrElse(0)))
    Media(
      epubs = epubs,
      images = images)
  }

  private def toModifiedAt(xml: NodeSeq): DateTime = {
    val modifiedAt = (xml \ "modifiedAt").trimText
    if (modifiedAt.isEmpty) throw MissingFieldException("modifiedAt")
    else ModifiedAtFormatter.parseDateTime(modifiedAt)
  }

  private def toDates(xml: NodeSeq): Option[Dates] = {
    val publishDate = (xml \ "publishedOn").trimText
    val announceDate = (xml \ "announcedOn").trimText
    Option(
      Dates(
        publish =
          if (publishDate.isEmpty) None
          else Some(PublishedOnFormatter.parseDateTime(publishDate)),
        announce =
          if (announceDate.isEmpty) None
          else Some(AnnouncedOnFormatter.parseDateTime(announceDate))
      )
    )
  }

  private def toReasons(xml: NodeSeq): List[String] =
    (xml  \ "reasonList" \ "reason").map(node => (node \ "description").trimText).toList

  private def toSource(xml: NodeSeq): Source = {
    val username = (xml \ "@{http://schemas.blinkbox.com/books/routing}originator").toString
    Source(
      deliveredAt = Option.empty,
      uri = Option.empty,
      fileName = Option.empty,
      contentType = Option.empty,
      role = Option.empty,
      username = username,
      system = Option.empty,
      processedAt = Option.empty
    )
  }
}

class PriceXmlV1IngestionParser extends XmlV1IngestionParser{
  override def produceDistributeContent(xml: Elem): PartialFunction[String, DistributeContent] = {
    case "book-price" => toBookPrice(xml)
    case _ => throw InvalidContentException(s"${xml.label}")
  }

  private def toBookPrice(xml: Elem): BookPrice = {
    val bookPriceXml = xml \\ "book-price"
    BookPrice(
      isbn = (bookPriceXml \ "isbn").trimText
        .opt[String]
        .getOrElse(throw MissingFieldException("isbn")),
      price = (bookPriceXml \ "price").trimText
        .opt[Double]
        .getOrElse(throw MissingFieldException("price")),
      currency = (bookPriceXml \ "currency").trimText
        .opt[String]
        .getOrElse(throw MissingFieldException("currency")))
  }
}