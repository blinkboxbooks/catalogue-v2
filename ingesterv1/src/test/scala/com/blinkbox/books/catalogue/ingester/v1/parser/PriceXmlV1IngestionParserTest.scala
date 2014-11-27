package com.blinkbox.books.catalogue.ingester.v1.parser

import com.blinkbox.books.test.MockitoSyrup
import org.scalatest.FlatSpecLike
import scala.util.Failure

class PriceXmlV1IngestionParserTest extends FlatSpecLike
  with MockitoSyrup{

  val parser = new PriceXmlV1IngestionParser

  it should "successfully parse a corrent formatted 'book-price' xml message" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("book-price.xml")

    val bookPrice = parser.parse(xmlContent)

    assert(bookPrice.isSuccess)
  }

  it should "fail parsing when 'isbn' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("isbn", asString("book-price.xml"))

    val bookPrice = parser.parse(xmlContent)

    val Failure(e: MissingFieldException) = bookPrice
    assert(e.field == "isbn")
  }

  it should "fail parsing when 'price' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("price", asString("book-price.xml"))

    val bookPrice = parser.parse(xmlContent)

    val Failure(e: MissingFieldException) = bookPrice
    assert(e.field == "price")
  }

  it should "fail parsing when 'currency' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("currency", asString("book-price.xml"))

    val bookPrice = parser.parse(xmlContent)

    val Failure(e: MissingFieldException) = bookPrice
    assert(e.field == "currency")
  }
}