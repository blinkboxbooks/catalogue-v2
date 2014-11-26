package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.Events.Book
import org.joda.time.DateTime

object MessageFixtures {
  val simpleBook = Book(
    sequenceNumber = 100,
    `$schema` = Some("book"),
    classification = Classification("book", "1234567890123") :: Nil,
    isbn = "1234567890123",
    format = None,
    title = "A simple book",
    subtitle = Some("How to be a very simple book"),
    contributors = Contributor("author", "abc123", "Foo C. Bar", "Bar Foo") :: Nil,
    availability = Some(
      BookAvailability(
        notificationType = None,
        publishingStatus = None,
        availabilityCode = None,
        productAvailability = None,
        blinkboxBooks = None
      )),
    dates = Some(Dates(Some(new DateTime()), None)),
    descriptions = OtherText(Nil, "The simple description of a simple book", "", Some("Contentus")) :: Nil,
    reviews = Nil,
    languages = "EN" :: Nil,
    originalLanguages = "EN" :: Nil,
    supplyRights = Some(Regions(Some(true), None, None)),
    salesRights = Some(Regions(Some(true), None, None)),
    publisher = Some("The Simplest Publisher"),
    imprint = None,
    prices = Price(
      amount = 12.3,
      currency =  "GBP",
      includesTax =  true,
      isAgency =  true,
      discountRate = None,
      validFrom = None,
      validUntil = None,
      applicableRegions = Some(Regions(Some(true), None, None)),
      tax = None
    ) :: Nil,
    statistics = None,
    subjects = Subject("a type", "acode", Some(true)) :: Nil,
    series = None,
    related = Nil,
    media = None,
    distributionStatus = DistributionStatus(true, Nil),
    source = Source(new DateTime(), None, None, None, "some-role", "some-username", None, None))
}
