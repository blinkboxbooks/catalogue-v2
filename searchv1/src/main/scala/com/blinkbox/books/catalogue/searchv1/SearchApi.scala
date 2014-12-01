package com.blinkbox.books.catalogue.searchv1

import akka.actor.ActorRefFactory
import com.blinkbox.books.catalogue.searchv1.V1SearchService.Book
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.v1.Version1JsonSupport
import com.blinkbox.books.spray.{Directives, _}
import org.slf4j.LoggerFactory
import spray.http.StatusCodes
import spray.routing._

class SearchApi(apiConfig: ApiConfig, searchService: V1SearchService)(implicit val actorRefFactory: ActorRefFactory)
    extends HttpService
    with Directives
    with Version1JsonSupport {

  implicit val log = LoggerFactory.getLogger(classOf[SearchApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  val searchDefaultCount = 50
  val similarDefaultCount = 10
  val suggestionsDefaultCount = 10

  val BookIdSegment = Segment.map(BookId.apply _)

  val serviceRoutes: Route = get {
    pathPrefix("catalogue" / "search") {
      pathPrefix("books") {
        pathEnd {
          get {
            parameter('q.?) { query =>
              query.fold[Route](complete(StatusCodes.BadRequest)) { query =>
                paged(searchDefaultCount) { page =>
                  onSuccess(searchService.search(query, page)) { res =>
                    complete(res)
                  }
                }
              }
            }
          }
        } ~
        path(BookIdSegment / "similar") { bookId =>
          get {
            paged(similarDefaultCount) { page =>
              complete(searchService.similar(bookId, page))
            }
          }
        }
      } ~
      path("suggestions") {
        get {
          parameter('q, 'limit.as[Int].?) { (q, limit) =>
            complete(searchService.suggestions(q, limit getOrElse suggestionsDefaultCount))
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
