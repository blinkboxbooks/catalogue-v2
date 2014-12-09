package com.blinkbox.books.catalogue.searchv1

import akka.actor.ActorRefFactory
import com.blinkbox.books.catalogue.searchv1.V1SearchService.PaginableResponse
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.{Directives, _}
import org.slf4j.LoggerFactory
import spray.http.{StatusCodes, Uri}
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing._

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

  val BookIdSegment = Segment.map(BookId.apply _)

  val completePaged: Page => PaginableResponse => StandardRoute = page => content => new StandardRoute {
    override def apply(ctx: RequestContext): Unit = ctx.complete(Paged(page, ctx.request.uri, content))
  }

  val serviceRoutes: Route = get {
    pathPrefix("catalogue" / "search") {
      pathPrefix("books") {
        pathEnd {
          get {
            parameter('q.?) { query =>
              query.fold[Route](complete(StatusCodes.BadRequest)) { query =>
                paged(searchDefaultCount) { page =>
                  onSuccess(searchService.search(query, page)) { res =>
                    completePaged(page)(res)
                  }
                }
              }
            }
          }
        } ~
        path(BookIdSegment / "similar") { bookId =>
          get {
            paged(similarDefaultCount) { page =>
              onSuccess(searchService.similar(bookId, page)) { res =>
                completePaged(page)(res)
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

  def routes: Route = rootPath(apiConfig.localUrl.path) {
    monitor() {
      serviceRoutes
    }
  }
}
