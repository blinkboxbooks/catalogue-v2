package com.blinkbox.books.catalogue.ingester.v1.parser

import com.blinkbox.books.catalogue.common.Events.{Undistribute, Book}
import com.blinkbox.books.test.MockitoSyrup
import org.scalatest.FlatSpecLike
import scala.util.{Success, Failure}

class BookXmlV1IngestionParserTest extends FlatSpecLike
  with MockitoSyrup{
  private val parser = new BookXmlV1IngestionParser

  it should "successfully parse a correct formatted 'book' xml message" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("book.xml")

    val book = parser.parse(xmlContent)

    book match {
      case Success(book: Book) =>
        book.media.fold(fail("Missing media")) { media =>
          assert(media.epubs.size == 2)
          assert(media.images.size == 1)
        }
      case els => fail(s"Expected a valid 'book', got $els")
    }
  }

  it should "fail parsing when incorrect xml format" in new XmlV1IngestionParserFixture {
    val xmlContent = """{"json": "instead", "of": "xml"}"""

    val book = parser.parse(xmlContent)

    book match {
      case Success(_) =>
        fail("Expected to fail parsing the book")
      case Failure(e) =>
        assert(e.isInstanceOf[InvalidContentException])
    }
  }

  it should "fail parsing when 'modifiedAt' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("modifiedAt", asString("book.xml"))

    val book = parser.parse(xmlContent)

    book match {
      case Success(_) =>
        fail("Expected to fail parsing the 'book' xml")
      case Failure(e: MissingFieldException) =>
        assert(e.field == "modifiedAt")
      case Failure(e) =>
        fail(s"Expected 'MissingFieldException(modifiedAt')', but got $e")
    }
  }

  it should "successfully parse a correct formatted 'undistribute' xml message" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("undistribute.xml")

    val undistribute = parser.parse(xmlContent)

    assert(undistribute.isSuccess)
  }

  it should "parse all reasons from an 'undistribute' xml" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("undistribute.xml")

    val undistribute = parser.parse(xmlContent)

    undistribute match {
      case Failure(e) =>
        fail(s"Expected to pass the parsing, but failed [$e]")
      case Success(undistribute: Undistribute) =>
        assert(undistribute.reasons.size == 1)
      case Success(c) =>
        fail(s"Expected to parse to 'undistribute', got [$c] ")
    }
  }

  it should "not fail when 'reasonList' field is missing for the 'undistribute' xml" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("reasonList", asString("undistribute.xml"))

    val undistribute = parser.parse(xmlContent)

    undistribute match {
      case Failure(e) =>
        fail(s"Expected to pass the parsing, but failed [$e]")
      case Success(undistribute: Undistribute) =>
        assert(undistribute.reasons.isEmpty)
      case Success(c) =>
        fail(s"Expected to parse to 'undistribute', got [$c] ")
    }
  }

  it should "fail parsing when 'effectiveTimestamp' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("effectiveTimestamp", asString("undistribute.xml"))

    val undistribute = parser.parse(xmlContent)

    undistribute match {
      case Success(_) =>
        fail("Expected to fail parsing the 'undistribute' xml")
      case Failure(e: MissingFieldException) =>
        assert(e.field == "effectiveTimestamp")
      case Failure(e) =>
        fail(s"Expected 'MissingFieldException(effectiveTimestamp')', but got $e")
    }
  }
}
