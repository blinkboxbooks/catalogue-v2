package com.blinkbox.books.catalogue.browser

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.blinkbox.books.catalogue.common.{ElasticFactory, SearchConfig}
import com.blinkbox.books.config.{ApiConfig, Configuration}
import com.blinkbox.books.json.DefaultFormats
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.{HttpServer, url2uri}
import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.json4s.Formats
import spray.can.Http
import spray.httpx.Json4sJacksonSupport
import spray.routing._

class WebService(searchService: V1SearchService) extends HttpServiceActor with Json4sJacksonSupport {
  implicit def json4sJacksonFormats: Formats = DefaultFormats
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  val BookIdSegment = Segment.map(BookId.apply _)

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
        path(BookIdSegment / "similar") { bookId =>
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
    val searchConfig = SearchConfig(config)

    implicit val actorSystem = ActorSystem("catalog-browser-system")
    implicit val executionContext = actorSystem.dispatcher
    implicit val startTimeout = Timeout(apiConfig.timeout)

    val client = ElasticFactory.remote(searchConfig)
    val searchService: V1SearchService = new EsV1SearchService(searchConfig, client)

    val service = actorSystem.actorOf(Props(classOf[WebService], searchService), "catalog-browser-actor")
    val localUrl = apiConfig.localUrl
    HttpServer(Http.Bind(service, localUrl.getHost, localUrl.effectivePort))

    sys.addShutdownHook(actorSystem.shutdown())
  }
}
