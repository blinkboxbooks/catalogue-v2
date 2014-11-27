package com.blinkbox.books.catalogue.ingester.v1.parser

import com.blinkbox.books.catalogue.common.Events.{Undistribute, Book}
import com.blinkbox.books.test.MockitoSyrup
import org.scalatest.FlatSpecLike
import scala.util.{Failure, Success}

class BookXmlV1IngestionParserTest extends FlatSpecLike
  with MockitoSyrup{
  private val parser = new BookXmlV1IngestionParser

  it should "successfully parse a correct formatted 'book' xml message" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("book.xml")

    val book = parser.parse(xmlContent)

    val Success(b: Book) = book
    b.media.fold(fail("Missing media")) { media =>
      assert(media.epubs.size == 2)
      assert(media.images.size == 1)
    }
  }

  it should "fail parsing when incorrect xml format" in new XmlV1IngestionParserFixture {
    val xmlContent = """{"json": "instead", "of": "xml"}"""

    val book = parser.parse(xmlContent)

    val Failure(e) = book
    assert(e.isInstanceOf[InvalidContentException])
  }

  it should "fail parsing when 'modifiedAt' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("modifiedAt", asString("book.xml"))

    val book = parser.parse(xmlContent)

    val Failure(e: MissingFieldException) = book
    assert(e.field == "modifiedAt")
  }

  it should "successfully parse a correct formatted 'undistribute' xml message" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("undistribute.xml")

    val undistribute = parser.parse(xmlContent)

    assert(undistribute.isSuccess)
  }

  it should "parse all reasons from an 'undistribute' xml" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("undistribute.xml")

    val undistribute = parser.parse(xmlContent)

    val Success(u: Undistribute) = undistribute
    assert(u.reasons.size == 1)
  }

  it should "not fail when 'reasonList' field is missing for the 'undistribute' xml" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("reasonList", asString("undistribute.xml"))

    val undistribute = parser.parse(xmlContent)

    val Success(u: Undistribute) = undistribute
    assert(u.reasons.isEmpty)
  }

  it should "fail parsing when 'effectiveTimestamp' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("effectiveTimestamp", asString("undistribute.xml"))

    val undistribute = parser.parse(xmlContent)

    val Failure(e: MissingFieldException) = undistribute
    assert(e.field == "effectiveTimestamp")
  }
}
