package com.blinkbox.books.catalogue.ingester.parser

import com.blinkbox.books.test.MockitoSyrup
import org.scalatest.FlatSpecLike
import scala.io.Source
import scala.util.{Success, Failure}

class XmlV1IngestionParserTest extends FlatSpecLike
  with MockitoSyrup{

  it should "successfully parse a correct formatted xml" in new XmlV1IngestionParserFixture {
    val xmlContent = asString("book.xml")

    val book = v1Parser.parse(xmlContent)

    book match {
      case Success(Left(book)) =>
        assert(book.media.epubs.size == 2)
        assert(book.media.images.size == 1)
      case els => fail(new RuntimeException(s"got $els"))
    }
  }

  it should "fail parsing when incorrect xml format" in new XmlV1IngestionParserFixture {
    val xmlContent = """{"json": "instead", "of": "xml"}"""

    val book = v1Parser.parse(xmlContent)

    assert(book.isFailure)
  }

  private class XmlV1IngestionParserFixture {
    def asString(xmlResource: String): String =
      Source.fromURL(getClass.getResource(s"/$xmlResource")).mkString

    val v1Parser = new XmlV1IngestionParser
  }

}