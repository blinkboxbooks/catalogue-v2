package com.blinkbox.books.agora.catalogue.contributor

import akka.actor.ActorRefFactory
import com.blinkbox.books.json.ExplicitTypeHints
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.v1.{ListPage, Version1JsonSupport}
import com.blinkbox.books.spray.{Directives => CommonDirectives, _}
import com.typesafe.scalalogging.StrictLogging
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import spray.routing.{ExceptionHandler, HttpService, Route}
import spray.util.LoggingContext
import scala.util.control.NonFatal
import com.blinkbox.books.config.ApiConfig
import scala.concurrent.Future

trait ContributorService {
  // TODO
  //def getContributorsById(ids: Iterable[String]): Future[ListPage[Contributor]]
  //def getContributorsByGroupName(groupName: String, groupParam: String, page: Page): Future[ListPage[Contributor]]
  def getContributorById(id: String): Future[Option[Contributor]]
}

trait ContributorRoutes extends HttpService {
  def getContributorById: Route
}

class ContributorApi(api: ApiConfig, config: ContributorConfig, service: ContributorService)
                    (implicit val actorRefFactory: ActorRefFactory) extends ContributorRoutes
  with CommonDirectives
  with Version1JsonSupport
  with StrictLogging {
  
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val timeout = api.timeout
  implicit val log = logger
  
  override val responseTypeHints = ExplicitTypeHints(Map(
    classOf[ListPage[_]] -> "urn:blinkboxbooks:schema:list",
    classOf[Contributor] -> "urn:blinkboxbooks:schema:contributor")
  )
  
  val idParam = "id"
  val groupParam = "groupname"

  val getContributorById = path(Segment) { id =>
    get {
      onSuccess(service.getContributorById(id))(cacheable(config.maxAge, _))
    }
  }

  val routes = rootPath(api.localUrl.path + config.path) {
    monitor() {
      respondWithHeader(RawHeader("Vary", "Accept, Accept-Encoding")) {
        getContributorById
      }
    }
  }

  private def exceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
    case NonFatal(e) =>
      log.error(e, "Unhandled error")
      uncacheable(InternalServerError, None)
  }
  
}
