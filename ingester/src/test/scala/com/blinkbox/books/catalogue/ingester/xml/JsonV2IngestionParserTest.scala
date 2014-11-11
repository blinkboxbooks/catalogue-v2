package com.blinkbox.books.catalogue.ingester.xml

import com.blinkbox.books.messaging._
import com.blinkbox.books.test.MockitoSyrup
import org.json4s.JsonAST.JValue
import org.scalatest.FlatSpecLike
import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.util.{Failure, Success}

class JsonV2IngestionParserTest extends FlatSpecLike
  with MockitoSyrup{

  it should "successfully validate a complete message" in new JsonV2IngestionParserFixture {
    val book = v2Parser.parse(toEventBody(completeJson))

    book match {
      case Success(b) =>
        assert(b.media.images.size == 1)
        assert(b.media.epubs.size == 2)
      case Failure(e) => fail(e)
    }
  }

  it should "fail validation when 'title' is missing" in new JsonV2IngestionParserFixture {
    val json = removeField(completeJson, "title")

    val book = v2Parser.parse(toEventBody(json))

    assert(book.isFailure)
  }

  it should "fail validation when 'available' field from 'availability' is missing" in new JsonV2IngestionParserFixture {
    val json = removeField(completeJson, "available")

    val book = v2Parser.parse(toEventBody(json))

    assert(book.isFailure)
  }

  it should "fail validation when 'availability' is missing" in new JsonV2IngestionParserFixture {
    val json = removeField(completeJson, "availability")

    val book = v2Parser.parse(toEventBody(json))

    assert(book.isFailure)
  }

  it should "fail validation when 'isbn' is missing" in new JsonV2IngestionParserFixture {
    val json = removeField(completeJson, "isbn")

    val book = v2Parser.parse(toEventBody(json))

    assert(book.isFailure)
  }

  private class JsonV2IngestionParserFixture {
    val completeJson = asJValue("distribute-book.json")
    implicit object JValueJson extends JsonEventBody[JValue] {
      val jsonMediaType = MediaType("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json")
    }

    def asJValue(jsonResource: String): JValue =
      parse(readFile(jsonResource))

    def asEventBody(jsonResource: String): EventBody =
      toEventBody(asJValue(jsonResource))

    def toEventBody(jvalue: JValue): EventBody =
      Event.json(
        EventHeader("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json"),
        jvalue).body

    private def readFile(resource: String): String =
      Source.fromURL(getClass.getResource(s"/$resource")).mkString

    val v2Parser = new JsonV2IngestionParser
  }

  private def removeField(json: JValue, fieldName: String): JValue =
    json.removeField{
      case JField(name, _) if name == fieldName => true
      case _ => false
    }
}
