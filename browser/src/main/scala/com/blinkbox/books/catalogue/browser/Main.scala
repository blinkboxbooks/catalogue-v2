package com.blinkbox.books.catalogue.browser

import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import com.blinkbox.books.config.{ApiConfig, Configuration}
import com.blinkbox.books.json.DefaultFormats
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.HttpServer
import org.json4s.Formats
import spray.can.Http
import spray.http.StatusCodes._
import spray.httpx.Json4sJacksonSupport
import spray.routing._
import com.blinkbox.books.spray.url2uri

class WebService extends HttpServiceActor with Json4sJacksonSupport {
  val searchService: V1SearchService = ???

  implicit def json4sJacksonFormats: Formats = DefaultFormats
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  val validatePasswordResetToken: Route = get {
    pathPrefix("catalogue" / "search") {
      pathPrefix("books") {
        pathEnd {
          get {
            parameter('q) { q =>
              onSuccess(searchService.search(q)) { res =>
                complete(res)
              }
            }
          }
        } ~
        path(IntNumber / "similar") { bookId =>
          get {
            complete(searchService.similar(bookId))
          }
        }
      } ~
      path("suggestions") {
        get {
          parameter('q) { q =>
            complete(searchService.suggestions(q))
          }
        }
      }
    }
  }

  def receive = runRoute(validatePasswordResetToken)
}

object Main extends Configuration {
  def main(args: Array[String]): Unit = {
    val Prefix = "service.catalog-browser.api.public"
    val apiConfig = ApiConfig(config, Prefix)

    implicit val actorSystem = ActorSystem("catalog-browser-system")
    implicit val executionContext = actorSystem.dispatcher
    implicit val startTimeout = Timeout(apiConfig.timeout)

    sys.addShutdownHook(actorSystem.shutdown())
    val service = actorSystem.actorOf(Props[WebService], "catalog-browser-actor")
    val localUrl = apiConfig.localUrl
    HttpServer(Http.Bind(service, localUrl.getHost, localUrl.effectivePort))
  }
}
