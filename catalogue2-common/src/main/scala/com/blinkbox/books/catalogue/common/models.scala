package com.blinkbox.books.catalogue.common

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
case class Source(deliveredAt: Option[DateTime], uri: Option[String], fileName: Option[String],
                  contentType: Option[String], role: Option[String], username: String,
                  system: Option[System], processedAt: Option[DateTime])

object Events {

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

  case class Undistribute(isbn: String,
                          sequenceNumber: Long,
                          reasons: List[String]) extends DistributeContent

  case class BookPrice(isbn: String, price: Double, currency: String) extends DistributeContent

  object Book {
    def empty = Book(
      sequenceNumber = 1,
      `$schema` = None,
      classification = List.empty,
      isbn = "",
      format = None,
      title = "",
      subtitle = Option.empty,
      contributors = List.empty,
      availability = None,
      dates = None,
      descriptions = List.empty,
      reviews = List.empty,
      languages = List.empty,
      originalLanguages = List.empty,
      supplyRights = None,
      salesRights = None,
      publisher = None,
      imprint = None,
      prices = List.empty[Price],
      statistics = None,
      subjects = List.empty,
      series = None,
      related = List.empty,
      media = None,
      distributionStatus = DistributionStatus(usable = true, List.empty),
      source = Source(
        deliveredAt = Option.empty,
        uri = Option.empty,
        fileName = Option.empty,
        contentType = Option.empty,
        role = Option.empty,
        username = "",
        system = Option.empty,
        processedAt = Option.empty))
  }
}
