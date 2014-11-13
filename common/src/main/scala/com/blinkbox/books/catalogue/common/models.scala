package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common

case class Classification(realm: String, id: String)
case class Availability(available: Boolean, code: String, extra: String)
case class Description(classification: List[Classification], content: String, `type`: String, author: String)
case class Subject(`type`: String, code: String)
case class Uri(`type`: String, uri: String, params: Option[String])
case class Epub(classification: List[Classification], uris: List[Uri], keyFile: Option[String], wordCount: Long, size: Long)
case class Image(classification: List[Classification], uris: List[Uri], width: Int, height: Int, size: Int)
case class Media(epubs: List[Epub], images: List[Image])
case class RegionalRights(`GB`: Option[Boolean], `ROW`: Option[Boolean], `WORLD`: Option[Boolean])
case class Tax(amount: Double)
case class Price(amount: Double, currency: String, includesTax: Boolean,
                 isAgency: Boolean, discountRate: Option[Int], tax: Option[Tax])
case class Series(title: String, number: Option[Int])
case class Contributor(role: String, id: String, displayName: String, sortName: String)
case class Book(distribute: Boolean,
                title: String, subtitle: Option[String],
                availability: Availability,  isbn: String,
                regionalRights: RegionalRights, publisher: String, media: Media,
                languages: List[String], descriptions: List[Description], subjects: List[Subject],
                prices: List[Price], series: Option[Series], contributors: List[Contributor])

case class Undistribute(isbn: String)

object Book {
  def empty = Book(
    distribute = true,
    title = "", subtitle = Option.empty[String],
    availability = Availability(available = false, "", ""),
    isbn = "",
    regionalRights = RegionalRights(Option.empty[Boolean], Option.empty[Boolean], Option.empty[Boolean]),
    publisher = "", media = Media(List.empty[Epub], List.empty[Image]),
    languages = List.empty[String], descriptions = List.empty[Description],
    subjects = List.empty[Subject], prices = List.empty[Price],
    series = Option.empty[Series], contributors = List.empty[Contributor])
}

object IndexEntities {

  sealed trait SuggestionPayload
  case class BookPayload(isbn: String, title: String, authors: List[String]) extends SuggestionPayload
  case class ContributorPayload(id: String, displayName: String) extends SuggestionPayload

  case class Suggestion(input: List[String], output: String, payload: SuggestionPayload)

  case class Book(title: String, subtitle: Option[String],
                  availability: Availability,  isbn: String,
                  regionalRights: RegionalRights, publisher: String, media: Media,
                  languages: List[String], descriptions: List[Description], subjects: List[Subject],
                  prices: List[Price], series: Option[Series], contributors: List[Contributor],
                  autoComplete: List[Suggestion])

  object Book {

    def buildSuggestions(book: common.Book): List[Suggestion] = {
      val authors = book.contributors.filter(_.role == "author")

      val authorDisplayNames = authors.map(_.displayName)

      val bookSuggestion: Suggestion = Suggestion(
        input = book.title :: authorDisplayNames,
        output = s"${book.title} - ${authorDisplayNames.mkString(", ")}",
        payload = BookPayload(book.isbn, book.title, authorDisplayNames)
      )

      val authorSuggestions: List[Suggestion] = authors.map { a =>
        Suggestion(
          input = a.displayName :: Nil,
          output = a.displayName,
          payload = ContributorPayload(a.id, a.displayName)
        )
      }

      bookSuggestion :: authorSuggestions
    }

    def fromMessage(msg: common.Book): Book = Book(
      title = msg.title,
      subtitle = msg.subtitle,
      availability = msg.availability,
      isbn = msg.isbn,
      regionalRights = msg.regionalRights,
      publisher = msg.publisher,
      media = msg.media,
      languages = msg.languages,
      descriptions = msg.descriptions,
      subjects = msg.subjects,
      prices = msg.prices,
      series = msg.series,
      contributors = msg.contributors,
      autoComplete = buildSuggestions(msg)
    )
  }
}
