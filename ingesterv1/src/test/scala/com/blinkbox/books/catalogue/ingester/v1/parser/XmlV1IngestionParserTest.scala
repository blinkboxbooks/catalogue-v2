package com.blinkbox.books.catalogue.ingester.v1.parser

import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.test.MockitoSyrup
import org.scalatest.FlatSpecLike
import scala.io.Source
import scala.util.{Success, Failure}

class XmlV1IngestionParserTest extends FlatSpecLike
  with MockitoSyrup{

  it should "successfully parse a correct formatted 'book' xml message" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("book.xml")

    val book = v1Parser.parse(xmlContent)

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

    val book = v1Parser.parse(xmlContent)

    book match {
      case Success(_) =>
        fail("Expected to fail parsing the book")
      case Failure(e) =>
        assert(e.isInstanceOf[InvalidContentException])
    }
  }

  it should "fail parsing when 'modifiedAt' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("modifiedAt", asString("book.xml"))

    val book = v1Parser.parse(xmlContent)

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

    val undistribute = v1Parser.parse(xmlContent)

    assert(undistribute.isSuccess)
  }

  it should "fail parsing when 'effectiveTimestamp' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("effectiveTimestamp", asString("undistribute.xml"))

    val undistribute = v1Parser.parse(xmlContent)

    undistribute match {
      case Success(_) =>
        fail("Expected to fail parsing the 'undistribute' xml")
      case Failure(e: MissingFieldException) =>
        assert(e.field == "effectiveTimestamp")
      case Failure(e) =>
        fail(s"Expected 'MissingFieldException(effectiveTimestamp')', but got $e")
    }
  }

  it should "successfully parse a corrent formatted 'book-price' xml message" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("book-price.xml")

    val bookPrice = v1Parser.parse(xmlContent)

    assert(bookPrice.isSuccess)
  }

  it should "fail parsing when 'isbn' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("isbn", asString("book-price.xml"))

    val bookPrice = v1Parser.parse(xmlContent)

    bookPrice match {
      case Success(_) =>
        fail("Expected to fail parsing the 'book-price' xml")
      case Failure(e: MissingFieldException) =>
        assert(e.field == "isbn")
      case Failure(e) =>
        fail(s"Expected 'MissingFieldException(isbn')', but got $e")
    }
  }

  it should "fail parsing when 'price' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("price", asString("book-price.xml"))

    val bookPrice = v1Parser.parse(xmlContent)

    bookPrice match {
      case Success(_) =>
        fail("Expected to fail parsing the 'book-price' xml")
      case Failure(e: MissingFieldException) =>
        assert(e.field == "price")
      case Failure(e) =>
        fail(s"Expected 'MissingFieldException(effectiveTimestamp')', but got $e")
    }
  }

  it should "fail parsing when 'currency' field is missing" in new XmlV1IngestionParserFixture {
    val xmlContent = removeNode("currency", asString("book-price.xml"))

    val bookPrice = v1Parser.parse(xmlContent)

    bookPrice match {
      case Success(_) =>
        fail("Expected to fail parsing the 'book-price' xml")
      case Failure(e: MissingFieldException) =>
        assert(e.field == "currency")
      case Failure(e) =>
        fail(s"Expected 'MissingFieldException(effectiveTimestamp')', but got $e")
    }
  }

  private class XmlV1IngestionParserFixture {
    def asString(xmlResource: String): String =
      Source.fromURL(getClass.getResource(s"/$xmlResource")).mkString

    val v1Parser = new XmlV1IngestionParser

    // A regex based naive implementation of
    // node replacement within xml.
    // !!! NOTE: this *should* not be used within production code
    //           as it's not optimal.
    def removeNode(nodeName: String, content: String): String =
      content.replaceAll(s"<$nodeName.*</$nodeName>", "")
  }
}