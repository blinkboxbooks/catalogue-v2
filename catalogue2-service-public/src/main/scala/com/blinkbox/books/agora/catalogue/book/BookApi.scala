package com.blinkbox.books.agora.catalogue.book

import akka.actor.ActorRefFactory
import com.blinkbox.books.json.ExplicitTypeHints
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.v1.{Error, ListPage, Version1JsonSupport}
import com.blinkbox.books.spray.{Directives => CommonDirectives, _}
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import spray.httpx.unmarshalling._
import spray.routing._
import scala.util.control.NonFatal
import com.blinkbox.books.config.ApiConfig

trait BookRoutes extends HttpService {
  def getBookByIsbn: Route
  def getBookSynopsis: Route
  def getBooks: Route
  def getRelatedBooks: Route
}

class BookApi(api: ApiConfig, config: BookConfig, service: BookService)
             (implicit val actorRefFactory: ActorRefFactory) extends BookRoutes
  with CommonDirectives
  with Version1JsonSupport
  with StrictLogging {
  
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val timeout = api.timeout
  implicit val log = logger
  
  override val responseTypeHints = ExplicitTypeHints(Map(
    classOf[ListPage[_]] -> "urn:blinkboxbooks:schema:list",
    classOf[BookRepresentation] -> "urn:blinkboxbooks:schema:book",
    classOf[BookSynopsis] -> "urn:blinkboxbooks:schema:synopsis")
  )

  implicit val DateTimeDeserializer = new FromStringDeserializer[DateTime] {
    def apply(value: String) =
      try Right(ISODateTimeFormat.date().parseDateTime(value))
      catch {
        case NonFatal(ex) => Left(MalformedContent("'%s' is not a valid yyyy-MM-dd date format value" format value, ex))
      }
  }

  val getBookByIsbn = path(Segment) { isbn =>
    get {
      onSuccess(service.getBookByIsbn(isbn))(cacheable(config.maxAge, _))
    }
  }

  val getBookSynopsis = path(Segment / "synopsis") { id =>
    get {
      onSuccess(service.getBookSynopsis(id))(cacheable(config.maxAge, _))
    }
  }

  val getRelatedBooks = path(Segment / "related") { id =>
    get {
      paged(defaultCount = 50) { page =>
        onSuccess(service.getRelatedBooks(id, page))(cacheable(config.maxAge, _))
      }
    }
  }
  
  val getBooks = pathEndOrSingleSlash {
    get {
      orderedAndPaged(defaultOrder = SortOrder("title", desc = false), defaultCount = 50) { (order, page) =>
        validateSortOrder(order.field) {
          parameter(BookService.minPubDateParam.as[DateTime].?, BookService.maxPubDateParam.as[DateTime].?) { (minPubDate, maxPubDate) =>
            validateDateParameters(minPubDate, maxPubDate) {
              cancelRejection(MissingQueryParamRejection(BookService.contributorParam)) {
                parameter(BookService.contributorParam) { contributorId =>
                  onSuccess(service.getBooksByContributor(contributorId, minPubDate, maxPubDate, page, order))(cacheable(config.maxAge, _))
                }
              }
            }
          } ~
          cancelRejection(MissingQueryParamRejection(BookService.idParam)) {
            parameterSeq { params =>
              val list = params.collect { case ("id", value) => value}
              if (list.nonEmpty) {
                if (list.size <= config.maxResults) {
                  onSuccess(service.getBooks(list, page))(cacheable(config.maxAge, _))
                }
                else
                  uncacheable(BadRequest, Error("max_results_exceeded", s"Max results exceeded: ${config.maxResults}"))
              } else reject(MissingQueryParamRejection(BookService.idParam))
            }
          }
        }
      }
    }
  }
  
  val routes = rootPath(api.localUrl.path + config.path) {
    monitor() {
      respondWithHeader(RawHeader("Vary", "Accept, Accept-Encoding")) {
        getBookByIsbn ~ getBookSynopsis ~ getBooks ~ getRelatedBooks
      }
    }
  }
  
  private val PermittedOrderVals = Seq("title", "sales_rank", "publication_date", "price", "sequential", "author")

  private def validateSortOrder(order: String) = validate(
    PermittedOrderVals.contains(order.toLowerCase),
    s"Permitted values for order: ${PermittedOrderVals.mkString(", ")}"
  )

  private def validateDateParameters(minDate: Option[DateTime], maxDate: Option[DateTime]) = validate(
    (minDate, maxDate) match {
      case (Some(min), Some(max)) => min.isBefore(max) || ( min == max)
      case _ => true
    },
    s"$service.minPubDateParam cannot be after $service.maxPubDateParam")
}
