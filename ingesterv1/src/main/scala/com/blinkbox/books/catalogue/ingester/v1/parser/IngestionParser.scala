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

case class InvalidContentException(content: String) extends RuntimeException
case class MissingFieldException(field: String) extends RuntimeException

class XmlV1IngestionParser extends IngestionParser[String, DistributeContent]{
  private val ModifiedAtFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z")
  private val PublishedOnFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  private val AnnouncedOnFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  override def parse(content: String): Try[DistributeContent] =
    Try(XML.loadString(content))
      .recover({case _ => throw InvalidContentException("Invalid xml document.")})
      .map(xml => xml.label match {
        case "book" => xml.toBook
        case "undistribute" => xml.toUndistribute
        case "book-price" => xml.toBookPrice
        case _ => throw InvalidContentException(s"${xml.label}")
      })

  private implicit class RichElem(value: Elem) {

    def toBook: Book = {
      val xmlBook = value \\ "book"
      val contributors = toContributors(xmlBook)
      Book.empty.copy(
        sequenceNumber = toModifiedAt(xmlBook).getMillis,
        isbn = (xmlBook \ "isbn").text,
        title = (xmlBook \ "title").text,
        subtitle = (xmlBook \ "subtitle").text.opt[String],
        series = toSeries(xmlBook),
        contributors = contributors,
        descriptions = toDescriptions(xmlBook, contributors),
        publisher = (xmlBook \ "publisher" \ "name").text.opt[String],
        prices = toPrices(xmlBook),
        subjects = toSubjects(xmlBook),
        languages = List((xmlBook \ "language").text),
        supplyRights = Option(toRegionalRights(xmlBook)),
        dates = toDates(xmlBook),
        media = Option(toMedia(xmlBook))
      )
    }

    def toUndistribute: Undistribute = {
      val undistributeXml = value \\ "undistribute"
      Undistribute(
        isbn = (undistributeXml \ "book").text,
        sequenceNumber = (undistributeXml \ "effectiveTimestamp")
          .text.opt[DateTime]
          .getOrElse(throw MissingFieldException("effectiveTimestamp"))
          .getMillis,
        reasons = toReasons(undistributeXml)
      )
    }

    def toBookPrice: BookPrice = {
      val bookPriceXml = value \\ "book-price"
      BookPrice(
        isbn = (bookPriceXml \ "isbn").text
          .opt[String]
          .getOrElse(throw MissingFieldException("isbn")),
        price = (bookPriceXml \ "price").text
          .opt[Double]
          .getOrElse(throw MissingFieldException("price")),
        currency = (bookPriceXml \ "currency").text
          .opt[String]
          .getOrElse(throw MissingFieldException("currency")))
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
      Option(
        Series(
          title = (xml \ "series" \ "title").text,
          number = (xml \ "series" \ "number").text.opt[Int]
        )
      )

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

    private def toRegionalRights(xml: NodeSeq): Regions = {
      val regions = (xml \ "regions").map(node => (node \ "region").text.toUpperCase)
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
      Media(
        epubs = epubs,
        images = images)
    }

    private def toModifiedAt(xml: NodeSeq): DateTime = {
      val modifiedAt = (xml \ "modifiedAt").text
      if(modifiedAt.isEmpty) throw MissingFieldException("modifiedAt")
      else ModifiedAtFormatter.parseDateTime(modifiedAt)
    }

    private def toDates(xml: NodeSeq): Option[Dates] = {
      val publishDate = (xml \ "publishedOn").text
      val announceDate = (xml \ "announcedOn").text
      Option(
        Dates(
          publish =
            if(publishDate.isEmpty) None
            else Some(PublishedOnFormatter.parseDateTime(publishDate)),
          announce =
            if(announceDate.isEmpty) None
            else Some(AnnouncedOnFormatter.parseDateTime(announceDate))
        )
      )
    }

    private def toReasons(xml: NodeSeq): List[String] =
      (xml  \ "reasonList" \ "reason").map(node => (node \ "description").text).toList
  }
}
