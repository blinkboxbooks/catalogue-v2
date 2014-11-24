package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common
import org.joda.time.DateTime

sealed trait DistributeContent
case class DistributionStatus(usable: Boolean, reasons: List[String])
case class Classification(realm: String, id: String)
case class Format(marvinIncompatible: Boolean, epubType: Option[String], productForm: Option[String])
case class Availability(available: Boolean, code: String, extra: String)
case class BookAvailability(notificationType: Option[Availability], publishingStatus: Option[Availability],
                            availabilityCode: Option[Availability], productAvailability: Option[Availability],
                            blinkboxBooks: Option[Availability])
case class OtherText(classification: List[Classification], content: String, `type`: String, author: Option[String])
case class Subject(`type`: String, code: String, main: Option[Boolean])
case class Uri(`type`: String, uri: String, params: Option[String])
case class Epub(classification: List[Classification], uris: List[Uri], keyFile: Option[String], wordCount: Long, size: Long)
case class Image(classification: List[Classification], uris: List[Uri], width: Int, height: Int, size: Int)
case class Media(epubs: List[Epub], images: List[Image])
case class Regions(`GB`: Option[Boolean], `ROW`: Option[Boolean], `WORLD`: Option[Boolean])
case class Tax(rate: String, percent: Option[Double], amount: Option[Double], taxableAmount: Option[Double])
case class Price(amount: Double, currency: String, includesTax: Boolean,
                 isAgency: Boolean, discountRate: Option[Int], validFrom: Option[DateTime],
                 validUntil: Option[DateTime], applicableRegions: Option[Regions], tax: Option[Tax])
case class Series(title: String, number: Option[Int])
case class Contributor(role: String, id: String, displayName: String, sortName: String)
case class Dates(publish: Option[DateTime], announce: Option[DateTime]) // TODO: publish date shouldn't be optional in V2
case class AdultThemes(rating: Double, reviewers: Int)
case class Statistics(pages: Option[Int], sentences: Option[Int], words: Option[Int],
                      syllables: Option[Int], polysyllables: Option[Int], smog_grade: Option[Int],
                      adultThemes: Option[AdultThemes])
case class Related(classification: Option[Classification], relation: Option[String], isbn: Option[String])
case class System(name: String, version: String)
case class Source(deliveredAt: DateTime, uri: Option[String], fileName: Option[String],
                  contentType: Option[String], role: String, username: String,
                  system: Option[System], processedAt: Option[DateTime])
case class Book(sequenceNumber: Long,
                `$schema`: Option[String],
                classification: List[Classification],
                isbn: String,
                format: Option[Format],
                title: String,
                subtitle: Option[String],
                contributors: List[Contributor],
                availability: Option[BookAvailability],
                dates: Option[Dates],
                descriptions: List[OtherText],
                reviews: List[OtherText],
                languages: List[String],
                originalLanguages: List[String],
                supplyRights: Option[Regions],
                salesRights: Option[Regions],
                publisher: Option[String],
                imprint: Option[String],
                prices: List[Price],
                statistics: Option[Statistics],
                subjects: List[Subject],
                series: Option[Series],
                related: List[Related],
                media: Option[Media],
                distributionStatus: DistributionStatus,
                source: Source) extends DistributeContent

case class Undistribute(isbn: String, effectiveTimestamp: DateTime) extends DistributeContent

object Book {
  def empty = Book(
    sequenceNumber = 1,
    `$schema` = None,
    classification = List.empty[Classification],
    isbn = "",
    format = None,
    title = "",
    subtitle = Option.empty[String],
    contributors = List.empty[Contributor],
    availability = None,
    dates = None,
    descriptions = List.empty[OtherText],
    reviews = List.empty[OtherText],
    languages = List.empty[String],
    originalLanguages = List.empty[String],
    supplyRights = None,
    salesRights = None,
    publisher = None,
    imprint = None,
    prices = List.empty[Price],
    statistics = None,
    subjects = List.empty[Subject],
    series = None,
    related = List.empty[Related],
    media = None,
    distributionStatus = DistributionStatus(usable = true, List.empty[String]),
    source = Source(DateTime.now, None, None, None, "NONE-ROLE", "NONE-USERNAME", None, None))
}

object IndexEntities {

  sealed trait SuggestionType
  object SuggestionType {
    case object Book extends SuggestionType
    case object Contributor extends SuggestionType
  }

  case class SuggestionPayload(`type`: SuggestionType, item: SuggestionItem)

  sealed trait SuggestionItem
  object SuggestionItem {
    case class Book(isbn: String, title: String, authors: List[String]) extends SuggestionItem
    case class Contributor(id: String, displayName: String) extends SuggestionItem
  }

  case class SuggestionField(input: List[String], output: String, payload: SuggestionPayload)

  case class Book(sequenceNumber: Long,
                  `$schema`: Option[String],
                  classification: List[Classification],
                  isbn: String,
                  format: Option[Format],
                  title: String,
                  subtitle: Option[String],
                  contributors: List[Contributor],
                  availability: Option[BookAvailability],
                  dates: Option[Dates],
                  descriptions: List[OtherText],
                  reviews: List[OtherText],
                  languages: List[String],
                  originalLanguages: List[String],
                  supplyRights: Option[Regions],
                  salesRights: Option[Regions],
                  publisher: Option[String],
                  imprint: Option[String],
                  prices: List[Price],
                  statistics: Option[Statistics],
                  subjects: List[Subject],
                  series: Option[Series],
                  related: List[Related],
                  media: Option[Media],
                  distributionStatus: DistributionStatus,
                  source: Source,
                  autoComplete: List[SuggestionField])

  object Book {

    def bookPayload(isbn: String, title: String, contributors: List[String]) =
      SuggestionPayload(SuggestionType.Book, SuggestionItem.Book(isbn, title, contributors))

    def contributorPayload(id: String, name: String) =
      SuggestionPayload(SuggestionType.Contributor, SuggestionItem.Contributor(id, name))

    def buildSuggestions(book: common.Book): List[SuggestionField] = {
      val authors = book.contributors.filter(_.role.toLowerCase == "author")

      val authorNames = authors.map(_.displayName)

      val bookSuggestion: SuggestionField = SuggestionField(
        input = book.title :: authorNames,
        output = s"${book.title} - ${authorNames.mkString(", ")}",
        payload = bookPayload(book.isbn, book.title, authorNames)
      )

      val authorSuggestions: List[SuggestionField] = authors.map { a =>
        SuggestionField(
          input = a.displayName :: Nil,
          output = a.displayName,
          payload = contributorPayload(a.id, a.displayName)
        )
      }

      bookSuggestion :: authorSuggestions
    }

    case class Wrapper(
      book: Book,
      autoComplete: List[SuggestionField])

    def fromMessage(msg: common.Book): Book = Book(
      sequenceNumber = msg.sequenceNumber,
      `$schema` = msg.`$schema`,
      classification = msg.classification,
      isbn = msg.isbn,
      format = msg.format,
      title = msg.title,
      subtitle = msg.subtitle,
      contributors = msg.contributors,
      availability = msg.availability,
      dates = msg.dates,
      descriptions = msg.descriptions,
      reviews = msg.reviews,
      languages = msg.languages,
      originalLanguages = msg.originalLanguages,
      supplyRights = msg.supplyRights,
      salesRights = msg.salesRights,
      publisher = msg.publisher,
      imprint = msg.imprint,
      prices = msg.prices,
      statistics = msg.statistics,
      subjects = msg.subjects,
      series = msg.series,
      related = msg.related,
      media = msg.media,
      distributionStatus = msg.distributionStatus,
      source = msg.source,
      autoComplete = buildSuggestions(msg))
  }
}
