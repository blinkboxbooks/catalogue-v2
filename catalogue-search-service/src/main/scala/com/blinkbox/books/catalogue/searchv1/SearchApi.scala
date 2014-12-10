package com.blinkbox.books.catalogue.searchv1

import akka.actor.ActorRefFactory
import com.blinkbox.books.catalogue.searchv1.V1SearchService.PaginableResponse
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.{Directives, _}
import org.slf4j.LoggerFactory
import spray.http.{StatusCodes, Uri}
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
  import java.util.concurrent.TimeUnit
  import scala.concurrent.duration._
  
  def apply(config: Config): SearchApiConfig = SearchApiConfig(
    config.getInt("searchDefaultCount"),
    config.getInt("similarDefaultCount"),
    config.getInt("suggestionsDefaultCount"),
    config.getDuration("maxAge", TimeUnit.MILLISECONDS).millis
  )
}

class SearchApi(apiConfig: ApiConfig, searchConfig: SearchApiConfig, searchService: V1SearchService)(implicit val actorRefFactory: ActorRefFactory)
    extends HttpService
    with Directives
    with Serialization {

  implicit val log = LoggerFactory.getLogger(classOf[SearchApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  val BookIdSegment = Segment.map(BookId.apply _)

  val completePaged: Page => PaginableResponse => StandardRoute = page => content => new StandardRoute {
    override def apply(ctx: RequestContext): Unit = ctx.complete(Paged(page, ctx.request.uri, content))
  }

  val serviceRoutes: Route = get {
    pathPrefix("catalogue" / "search") {
      pathPrefix("books") {
        pathEnd {
          get {
            parameter('q ? "") { q =>
              val query = q.trim
              validate(!query.isEmpty, "Missing search query term") {
                paged(searchConfig.searchDefaultCount) { page =>
                  onSuccess(searchService.search(query, page)) { res =>
                    cacheable(searchConfig.maxAge) {
                      completePaged(page)(res)
                    }
                  }
                }
              }
            }
          }
        } ~
        path(Segment / "similar") { rawBookId =>
          validate(rawBookId.forall(_.isDigit) && rawBookId.length == 13, s"Invalid ID: $rawBookId") {
            get {
              paged(searchConfig.similarDefaultCount) { page =>
                onSuccess(searchService.similar(BookId(rawBookId), page)) { res =>
                  cacheable(searchConfig.maxAge) {
                    completePaged(page)(res)
                  }
                }
              }
            }
          }
        }
      } ~
      path("suggestions") {
        get {
          parameter('q, 'limit.as[Int].?) { (q, limit) =>
            validate(limit.fold(true)(_ > 0), "The limit parameter must be greater than 0 if provided") {
              cacheable(searchConfig.maxAge) {
                complete(searchService.suggestions(q, limit getOrElse searchConfig.suggestionsDefaultCount))
              }
            }
          }
        }
      }
    }
  }

  val rejectionHandler = RejectionHandler {
    case ValidationRejection(message, _) :: _ => complete(StatusCodes.BadRequest, message)
  }

  val exceptionHandler = ExceptionHandler {
    case NonFatal(ex) => uncacheable(StatusCodes.InternalServerError, s"Unknown error: ${ex.getMessage}")
  }

  def routes: Route = rootPath(apiConfig.localUrl.path) {
    monitor(log) {
      handleExceptions(exceptionHandler) {
        handleRejections(rejectionHandler) {
          serviceRoutes
        }
      }
    }
  }
}
