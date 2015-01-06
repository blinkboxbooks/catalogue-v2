package com.blinkbox.books.catalogue.searchv1

import akka.actor.ActorRefFactory
import com.blinkbox.books.catalogue.searchv1.V1SearchService.PaginableResponse
import com.blinkbox.books.config.{ ApiConfig, RichConfig }
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.{ Directives, _ }
import com.typesafe.scalalogging.StrictLogging
import shapeless.HNil
import spray.http.{ MediaTypes, StatusCodes, Uri }
import spray.httpx.marshalling.BasicMarshallers
import spray.routing._
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

class Paged[T](val page: Page, val uri: Uri, val numberOfResults: Long, val content: T)

object Paged {
  def apply(page: Page, uri: Uri, response: PaginableResponse) = new Paged(page, uri, response.numberOfResults, response)
}

case class SearchApiConfig(
  searchDefaultCount: Int,
  similarDefaultCount: Int,
  suggestionsDefaultCount: Int,
  maxAge: FiniteDuration
)

object SearchApiConfig {
  import com.typesafe.config.Config

  def apply(config: Config): SearchApiConfig = SearchApiConfig(
    config.getInt("searchDefaultCount"),
    config.getInt("similarDefaultCount"),
    config.getInt("suggestionsDefaultCount"),
    config.getFiniteDuration("maxAge")
  )
}

class SearchApi(apiConfig: ApiConfig, searchConfig: SearchApiConfig, searchService: V1SearchService)(implicit val actorRefFactory: ActorRefFactory)
  extends HttpService
  with Directives
  with Serialization
  with StrictLogging{

  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val log = logger

  val BookIdSegment = Segment.map(BookId.apply _)

  val completePaged: Page => PaginableResponse => StandardRoute = page => content => new StandardRoute {
    override def apply(ctx: RequestContext): Unit = ctx.complete(Paged(page, ctx.request.uri.toRelative, content))
  }

  val validSpecialChars = "-,.';!"
  def preProcess(q: String): String = q.filter(c => c.isLetterOrDigit || validSpecialChars.contains(c) || c.isWhitespace).trim

  val validatedQuery = parameter('q ? "").flatMap { raw =>
    validate(!raw.trim.isEmpty, "Missing search query term").hflatMap { _ =>
      val q = preProcess(raw)
      validate(!q.isEmpty, "Invalid or empty search term").hflatMap { _ =>
        hprovide(raw :: q :: HNil)
      }
    }
  }

  val bookSearchRoute =
    orderedAndPaged(defaultOrder = SortOrder(field = "relevance", desc = true), defaultCount = searchConfig.searchDefaultCount) { (order, page) =>
      validateSortOrder(order.field) {
        validatedQuery { (raw, query) =>
          onSuccess(searchService.search(query, page, order)) { res =>
            cacheable(searchConfig.maxAge) {
              completePaged(page)(res.copy(id = raw))
            }
          }
        }
      }
    }

  def similarBooksRoute(rawBookId: String) =
    validate(rawBookId.forall(_.isDigit) && rawBookId.length == 13, s"Invalid ID: $rawBookId") {
      paged(searchConfig.similarDefaultCount) { page =>
        onSuccess(searchService.similar(BookId(rawBookId), page)) { res =>
          cacheable(searchConfig.maxAge) {
            completePaged(page)(res)
          }
        }
      }
    }

  val suggestionsRoute =
    parameter('q, 'limit.as[Int].?) { (q, limit) =>
      validate(limit.fold(true)(_ > 0), "The limit parameter must be greater than 0 if provided") {
        cacheable(searchConfig.maxAge) {
          complete(searchService.suggestions(q, limit getOrElse searchConfig.suggestionsDefaultCount))
        }
      }
    }

  val serviceRoutes: Route = get {
    pathPrefix("catalogue" / "search") {
      pathPrefix("books") {
        pathEnd(bookSearchRoute) ~ path(Segment / "similar")(similarBooksRoute)
      } ~
      path("suggestions")(suggestionsRoute)
    }
  }

  def rejectionHandler = RejectionHandler {
    case ValidationRejection(message, _) :: _ =>
      implicit val marshaller = BasicMarshallers.StringMarshaller

      respondWithMediaType(MediaTypes.`text/plain`) {
        complete(StatusCodes.BadRequest, message)
      }
  }

  def exceptionHandler = ExceptionHandler {
    case NonFatal(ex) =>
      logger.error(s"Unknown error: ${ex.getMessage}", ex)
      uncacheable(StatusCodes.InternalServerError, s"Unknown error: ${ex.getMessage}")
  }

  def routes: Route = rootPath(apiConfig.localUrl.path) {
    monitor() {
      handleExceptions(exceptionHandler) {
        handleRejections(rejectionHandler) {
          serviceRoutes
        }
      }
    }
  }

  private val PermittedOrderVals = Seq("relevance", "author", "popularity", "price", "publication_date")

  private def validateSortOrder(order: String) = validate(
    PermittedOrderVals.contains(order.toLowerCase),
    s"Permitted values for order: ${PermittedOrderVals.mkString(", ")}"
  )
}
