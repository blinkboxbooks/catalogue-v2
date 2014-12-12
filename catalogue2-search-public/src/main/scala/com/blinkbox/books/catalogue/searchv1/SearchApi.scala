package com.blinkbox.books.catalogue.searchv1

import akka.actor.ActorRefFactory
import com.blinkbox.books.catalogue.searchv1.V1SearchService.PaginableResponse
import com.blinkbox.books.config.{ApiConfig, RichConfig}
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.{Directives, _}
import org.slf4j.LoggerFactory
import spray.http.{MediaTypes, StatusCodes, Uri}
import spray.httpx.marshalling.BasicMarshallers
import spray.routing._
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import com.blinkbox.books.catalogue.common.search.ElasticSearchSupport.validateSortOrder

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
    with Serialization {

  implicit val log = LoggerFactory.getLogger(classOf[SearchApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  val BookIdSegment = Segment.map(BookId.apply _)

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
            orderedAndPaged(defaultOrder = SortOrder(field="relevance", desc=true), defaultCount = searchConfig.searchDefaultCount) { (order, page) =>
              validateSortOrder(order.field) {
                parameter('q ? "") { q =>
                  validate(!q.trim.isEmpty, "Missing search query term") {
                    val query = preProcess(q)
                    validate(!query.isEmpty, "Invalid or empty search term") {
                      onSuccess(searchService.search(query, page, order)) { res =>
                        cacheable(searchConfig.maxAge) {
                          completePaged(page)(res.copy(id = q))
                        }
                      }
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
    case ValidationRejection(message, _) :: _ =>
      implicit val marshaller = BasicMarshallers.StringMarshaller

      respondWithMediaType(MediaTypes.`text/plain`) {
        complete(StatusCodes.BadRequest, message)
      }
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
