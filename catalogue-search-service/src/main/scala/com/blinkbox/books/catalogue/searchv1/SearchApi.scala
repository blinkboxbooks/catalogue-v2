package com.blinkbox.books.catalogue.searchv1

import akka.actor.ActorRefFactory
import com.blinkbox.books.catalogue.searchv1.V1SearchService.PaginableResponse
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.{Directives, _}
import org.slf4j.LoggerFactory
import spray.http.{StatusCodes, Uri}
import spray.routing._

import scala.util.control.NonFatal

class Paged[T](val page: Page, val uri: Uri, val numberOfResults: Long, val content: T)
object Paged {
  def apply(page: Page, uri: Uri, response: PaginableResponse) = new Paged(page, uri, response.numberOfResults, response)
}

class SearchApi(apiConfig: ApiConfig, searchService: V1SearchService)(implicit val actorRefFactory: ActorRefFactory)
    extends HttpService
    with Directives
    with Serialization {

  implicit val log = LoggerFactory.getLogger(classOf[SearchApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  val searchDefaultCount = 50
  val similarDefaultCount = 10
  val suggestionsDefaultCount = 10

  val completePaged: Page => PaginableResponse => StandardRoute = page => content => new StandardRoute {
    override def apply(ctx: RequestContext): Unit = ctx.complete(Paged(page, ctx.request.uri, content))
  }

  val validSpecialChars = "-,.';!"
  def preProcess(q: String): String = q.filter(c => c.isLetterOrDigit || validSpecialChars.contains(c) || c.isWhitespace).trim

  val serviceRoutes: Route = get {
    pathPrefix("catalogue" / "search") {
      pathPrefix("books") {
        pathEnd {
          get {
            parameter('q ? "") { q =>
              validate(!q.trim.isEmpty, "Missing search query term") {
                val query = preProcess(q)

                validate(!query.isEmpty, "Invalid or empty search term") {
                  paged(searchDefaultCount) { page =>
                    onSuccess(searchService.search(query, page)) { res =>
                      completePaged(page)(res.copy(id = q))
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
              paged(similarDefaultCount) { page =>
                onSuccess(searchService.similar(BookId(rawBookId), page)) { res =>
                  completePaged(page)(res)
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
              complete(searchService.suggestions(q, limit getOrElse suggestionsDefaultCount))
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
