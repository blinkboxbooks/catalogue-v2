package com.blinkbox.books.catalogue.common

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
                  autoComplete: List[SuggestionField],
                  descriptionContents: List[String])

  object Book {

    def bookPayload(isbn: String, title: String, contributors: List[String]) =
      SuggestionPayload(SuggestionType.Book, SuggestionItem.Book(isbn, title, contributors))

    def contributorPayload(id: String, name: String) =
      SuggestionPayload(SuggestionType.Contributor, SuggestionItem.Contributor(id, name))

    def buildSuggestions(book: Events.Book): List[SuggestionField] = {
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

    def buildMoreLikeThis(book: Events.Book): List[String] = {
      book.descriptions.map(_.content)
    }

    def fromMessage(msg: Events.Book): Book = Book(
      sequenceNumber = msg.sequenceNumber,
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
      autoComplete = buildSuggestions(msg),
      descriptionContents = buildMoreLikeThis(msg))
  }

  case class Undistribute(isbn: String, distributionStatus: DistributionStatus, sequenceNumber: Long)

  object Undistribute {
    def fromMessage(msg: Events.Undistribute): Undistribute = Undistribute(
      isbn = msg.isbn,
      distributionStatus = DistributionStatus(
        usable = false,
        reasons = msg.reasons
      ),
      sequenceNumber = msg.sequenceNumber
    )
  }

  case class BookPrice(isbn: String, price: Double, currency: String)

  object BookPrice {
    def fromMessage(msg: Events.BookPrice): BookPrice = BookPrice(
      isbn = msg.isbn,
      price = msg.price,
      currency = msg.currency
    )
  }
}
