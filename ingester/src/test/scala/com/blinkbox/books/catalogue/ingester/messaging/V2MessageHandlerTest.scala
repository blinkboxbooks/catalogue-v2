package com.blinkbox.books.catalogue.ingester.messaging

import akka.actor.{Status, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.blinkbox.books.catalogue.ingester.Distribute.Book
import com.blinkbox.books.catalogue.ingester.index.Search
import com.blinkbox.books.catalogue.ingester.xml.IngestionParser
import com.blinkbox.books.messaging._
import com.blinkbox.books.test.MockitoSyrup
import com.typesafe.config.ConfigFactory
import org.json4s.JsonAST.{JString, JField, JValue}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpecLike

import scala.concurrent.Future

class V2MessageHandlerTest extends TestKit(ActorSystem("test-system", ConfigFactory.parseString("""akka.loglevel = OFF""")))
  with ImplicitSender
  with FlatSpecLike
  with MockitoSyrup {

  import org.json4s.JsonDSL._

  it should "fail validation when 'title' is missing" in new MessageHandlerFixture {
    val json = removeField(completeJson, "title")
    val event = createEvent(json)
    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(eql(event), isA(classOf[RuntimeException]))
  }

  it should "fail validation when 'title' is not a string" in new MessageHandlerFixture {
    val json = completeJson.mapField{
      case JField("title", _) => JField("title", JField("test", JString("value")))
      case field => field
    }
    val event = createEvent(json)
    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(eql(event), isA(classOf[RuntimeException]))
  }

  it should "fail validation when 'available' field from 'availability' is missing" in new MessageHandlerFixture {
    val json = removeField(completeJson, "available")
    val event = createEvent(json)
    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(eql(event), isA(classOf[RuntimeException]))
  }

  it should "fail validation when 'availability' is missing" in new MessageHandlerFixture {
    val json = removeField(completeJson, "availability")
    val event = createEvent(json)
    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(eql(event), isA(classOf[RuntimeException]))
  }

  it should "fail validation when 'isbn' is missing" in new MessageHandlerFixture {
    val json = removeField(completeJson, "isbn")
    val event = createEvent(json)
    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(eql(event), isA(classOf[RuntimeException]))
  }

  it should "fail validation when 'regionalRights' is missing" in new MessageHandlerFixture {
    val json= removeField(completeJson, "regionalRights")
    val event = createEvent(json)
    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(eql(event), isA(classOf[RuntimeException]))
  }

  it should "fail validation when 'regionalRights' 'GB' is missing" in new MessageHandlerFixture {
    val json= removeField(completeJson, "GB")
    val event = createEvent(json)
    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(eql(event), isA(classOf[RuntimeException]))
  }

  private class MessageHandlerFixture {
    import scala.concurrent.duration._
    val search = mock[Search]
    val errorHandler = mock[ErrorHandler]
    val messageParser = mock[IngestionParser[EventBody, Book]]
    val retryInterval = 100.millis
    doReturn(Future.successful(())).when(errorHandler).handleError(any[Event], any[Throwable])
    val messageHandler = TestActorRef(Props(new V2MessageHandler(errorHandler, retryInterval, search, messageParser)))
  }

  private lazy val completeJson = {
    titleField ~
    availabilityField ~
    isbnField ~
    regionalRightsField ~
    publisherField ~
    mediaField ~
    languagesField ~
    descriptionsField ~
    subjectsField
  }

  private lazy val titleField =
    "title" -> "Little park bear"

  private lazy val availabilityField =
    "availability" ->
      availableField ~
      ("code" -> "PB") ~
      ("extra" -> "Bear cub")

  private lazy val availableField =
    "available" -> true

  private lazy val isbnField =
    "isbn" -> "9780007236893"

  private lazy val regionalRightsField =
    "regionalRights" ->
      ("GB" -> true) ~
      ("IE" -> true) ~
      ("ROW" -> false)

  private lazy val publisherField =
    "publisher" -> "PamPublish"

  private lazy val mediaField =
    "media" ->
      imagesField ~ epubsField

  private lazy val languagesField =
    "languages" -> List("eng")

  private lazy val descriptionsField =
    "descriptions" -> List(
      ("classification" -> List(
        ("realm" -> "Test1") ~
          ("id" -> "ID1"),
        ("realm" -> "Test2") ~
          ("id" -> "ID2"))) ~
        ("content" -> "This is a simple description of this book ... or something like that.") ~
        ("type" -> "45") ~
        authorField)

  private lazy val subjectsField =
    "subjects" -> List(
      ("type" -> "BISAC") ~
      ("code" -> "FIC050000"))

  private lazy val imagesField =
    "images" -> List(
      ("classification" -> List(
        ("realm" -> "type") ~
          ("id" -> "front_cover"))) ~
        ("uris" -> List(
          ("type" -> "resource_server") ~
            ("uri" -> "https://media.blinkboxbooks.com/path/to/file.png"),
          ("type" -> "static") ~
            ("uri" -> "http://container.azure.com/path/to/file.jpg"),
          ("type" -> "static") ~
            ("uri" -> "http://container.azure.com/path/to/file.jpg") ~
            ("params" -> "img:m=scale;img:w=300;v=0"))) ~
        ("width" -> 1200) ~
        ("height" -> 2500) ~
        ("size" -> 25485))

  private lazy val epubsField =
    "epubs" -> List(
      ("classification" -> List(
        ("realm" -> "type") ~
          ("id" -> "full_bbbdrm"))) ~
        ("uris" -> List(
          ("type" -> "static") ~
            ("uri" -> "http://container.azure.com/path/to/file.epub"),
          ("type" -> "resource_server") ~
            ("uri" -> "https://media.blinkboxbooks.com/path/to/file.epub"))) ~
        ("keyfile" -> "https://keys.blinkboxbooks.com/path/to/keyfile.epub.9780111222333.key") ~
        ("wordCount" -> 37462) ~
        ("size" -> 25485),
      ("classification" -> List(
        ("realm" -> "type") ~
          ("id" -> "sample"))) ~
        ("uris" -> List(
          ("type" -> "resource_server") ~
            ("uri" -> "https://media.blinkboxbooks.com/path/to/file.sample.epub"))) ~
        ("wordcount" -> 3746) ~
        ("size" -> 2548))

  private lazy val authorField =
    "author" -> "TheAuthhhhor"

  private def createEvent(json: JValue): Event = {
    implicit object JObjectJson extends JsonEventBody[JValue] {
      val jsonMediaType = MediaType("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json")
    }
    Event.json(EventHeader("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json"), json: JValue)
  }

  private def removeField(json: JValue, fieldName: String): JValue =
    json.removeField{
      case JField(name, _) if name == fieldName => true
      case _ => false
    }

}
