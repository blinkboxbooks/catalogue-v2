package com.blinkbox.books.catalogue.ingester.v1.messaging

import java.io.IOException
import java.net.ConnectException
import akka.actor.{Status, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common.DistributeContent
import com.blinkbox.books.catalogue.common.search.{CommunicationException, SingleResponse, Indexer}
import com.blinkbox.books.catalogue.ingester.v1.parser.IngestionParser
import com.blinkbox.books.messaging._
import com.blinkbox.books.test.MockitoSyrup
import com.typesafe.config.ConfigFactory
import org.json4s.JsonAST.JValue
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpecLike
import scala.concurrent.Future
import scala.util.Success

class MessageHandlerTest extends TestKit(ActorSystem("test-system", ConfigFactory.parseString("""akka.loglevel = OFF""")))
  with ImplicitSender
  with FlatSpecLike
  with MockitoSyrup {

  it should "notify error handler when not able to index the incoming message" in new MessageHandlerFixture {
    val event = createEvent(dummyJValue)
    val book = Book.empty
    when(indexer.index(book)).thenReturn(Future.failed(new RuntimeException("test exception")))
    when(messageParser.parse(bookContent)).thenReturn(Success(book))

    messageHandler ! event

    withSuccess {
      verify(errorHandler).handleError(any(), isA(classOf[RuntimeException]))
    }
  }

  it should "not notify error handler when ConnectException is raised" in new MessageHandlerFixture {
    val event = createEvent(dummyJValue)
    val book = Book.empty
    when(indexer.index(book))
      .thenReturn(Future.failed(new ConnectException))
      .thenReturn(Future.successful(SingleResponse(docId = "")))
    when(messageParser.parse(bookContent)).thenReturn(Success(book))

    messageHandler ! event

    withSuccess {
      verifyZeroInteractions(errorHandler)
      verify(messageParser, times(2)).parse(bookContent)
    }
  }

  it should "not notify error handler when IOException is raised" in new MessageHandlerFixture {
    val event = createEvent(dummyJValue)
    val book = Book.empty
    when(indexer.index(book))
      .thenReturn(Future.failed(new IOException))
      .thenReturn(Future.successful(SingleResponse(docId = "")))
    when(messageParser.parse(bookContent)).thenReturn(Success(book))

    messageHandler ! event

    withSuccess {
      verifyZeroInteractions(errorHandler)
      verify(messageParser, times(2)).parse(bookContent)
    }
  }

  it should "not notify error handler when CommunicationException is raised" in new MessageHandlerFixture {
    val event = createEvent(dummyJValue)
    val book = Book.empty
    when(indexer.index(book))
      .thenReturn(Future.failed(CommunicationException(new RuntimeException)))
      .thenReturn(Future.successful(SingleResponse(docId = "")))
    when(messageParser.parse(bookContent)).thenReturn(Success(book))

    messageHandler ! event

    withSuccess{
      verifyZeroInteractions(errorHandler)
      verify(messageParser, times(2)).parse(bookContent)
    }
  }

  it should "not notify error handler when able to index the incoming message" in new MessageHandlerFixture {
    val event = createEvent(dummyJValue)
    val book = Book.empty
    when(indexer.index(book)).thenReturn(Future.successful(SingleResponse(docId = "")))
    when(messageParser.parse(bookContent)).thenReturn(Success(book))

    messageHandler ! event

    withSuccess {
      verifyZeroInteractions(errorHandler)
    }
  }

  private abstract class MessageHandlerFixture {
    import scala.concurrent.duration._

    val errorHandler = mock[ErrorHandler]
    val messageParser = mock[IngestionParser[String, DistributeContent]]
    val indexer = mock[Indexer]
    val retryInterval = 100.millis
    val messageHandler = TestActorRef(Props(new MessageHandler(errorHandler, retryInterval, indexer, messageParser)))

    doReturn(Future.successful(())).when(errorHandler).handleError(any[Event], any[Throwable])

    def event: Event

    def bookContent = new String(event.body.content, "UTF-8")

    def withSuccess[T](block: => T): T = {
      expectMsgType[Status.Success]
      block
    }
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
