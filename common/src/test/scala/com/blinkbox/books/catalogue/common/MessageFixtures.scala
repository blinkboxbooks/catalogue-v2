package com.blinkbox.books.catalogue.common

import org.joda.time.DateTime

object MessageFixtures {
  val simpleBook = Book(
    distribute = true,
    title = "A simple book",
    subtitle = Some("How to be a very simple book"),
    availability = Availability(true, "UK", "Some extra"),
    isbn = "1234567890123",
    regionalRights = RegionalRights(Some(true), None, None),
    publisher = "The Simplest Publisher",
    media = Media(Nil, Nil),
    languages = "EN" :: Nil,
    descriptions = Description(Nil, "The simple description of a simple book", "", "Contentus") :: Nil,
    subjects = Subject("a type", "acode") :: Nil,
    prices = Price(12.3, "GBP", true, true, None, None) :: Nil,
    series = None,
    contributors = Contributor("author", "abc123", "Foo C. Bar", "Bar Foo") :: Nil,
    dates = Dates(Some(new DateTime()), None),
    modifiedAt = new DateTime()
  )
}

