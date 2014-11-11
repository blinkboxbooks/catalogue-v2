package com.blinkbox.books.catalogue.ingester.messaging

import akka.actor.{Status, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.blinkbox.books.catalogue.common.{EsIndexerTypes, Indexer, Book}
import com.blinkbox.books.catalogue.ingester.xml.IngestionParser
import com.blinkbox.books.messaging._
import com.blinkbox.books.test.MockitoSyrup
import com.typesafe.config.ConfigFactory
import org.json4s.JsonAST.JValue
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpecLike
import scala.concurrent.Future
import scala.util.Success

class V2MessageHandlerTest extends TestKit(ActorSystem("test-system", ConfigFactory.parseString("""akka.loglevel = OFF""")))
  with ImplicitSender
  with FlatSpecLike
  with MockitoSyrup {

  it should "notify error handler when not able to parse the incoming message" in new MessageHandlerFixture {
    val event = createEvent(dummyJValue)
    val book = Book.empty
    val indexFuture = Future.failed(new RuntimeException("test exception"))
    when(messageParser.parse(event.body)).thenReturn(Success(book))

    messageHandler ! event

    expectMsgType[Status.Success]
    verify(errorHandler).handleError(any(), isA(classOf[RuntimeException]))
  }

  private class MessageHandlerFixture {
    import scala.concurrent.duration._
    val errorHandler = mock[ErrorHandler]
    val messageParser = mock[IngestionParser[EventBody, Book]]
    val indexer = mock[Indexer[EsIndexerTypes]]
    val retryInterval = 100.millis
    doReturn(Future.successful(())).when(errorHandler).handleError(any[Event], any[Throwable])
    val messageHandler = TestActorRef(Props(new V2MessageHandler(errorHandler, retryInterval, indexer, messageParser)))
  }

  private def createEvent(json: JValue): Event = {
    implicit object JValueJson extends JsonEventBody[JValue] {
      val jsonMediaType = MediaType("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json")
    }
    Event.json(EventHeader("application/vnd.blinkbox.books.ingestion.book.metadata.v2+json"), json: JValue)
  }

  private def dummyJValue: JValue = {
    import org.json4s.JsonDSL._
    "field" -> "value"
  }
}
