package com.blinkbox.books.agora.catalogue.contributor

import akka.actor.ActorRefFactory
import com.blinkbox.books.json.ExplicitTypeHints
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.v1.{Error, ListPage, Version1JsonSupport}
import com.blinkbox.books.spray.{Directives => CommonDirectives, _}
import org.slf4j.LoggerFactory
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import spray.routing.{ExceptionHandler, HttpService, Route}
import spray.util.LoggingContext
import scala.util.control.NonFatal
import scala.concurrent.duration.FiniteDuration
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import com.blinkbox.books.config.ApiConfig
import scala.concurrent.Future

/**
 * Contributor service.
 */
trait ContributorService {
  // TODO
  //def getContributorsById(ids: Iterable[String]): Future[ListPage[Contributor]]
  //def getContributorsByGroupName(groupName: String, groupParam: String, page: Page): Future[ListPage[Contributor]]
  def getContributorById(id: String): Future[Option[Contributor]]
}

trait ContributorRoutes extends HttpService {
  //def getContributors: Route
  def getContributorById: Route
}

/*
trait ContributorGroupRoutes extends HttpService {
  def getContributorGroups: Route
  def getContributorGroupById: Route
}
*/

/**
 * 
 */
class ContributorApi(api: ApiConfig, config: ContributorConfig, service: ContributorService)
                    (implicit val actorRefFactory: ActorRefFactory) extends ContributorRoutes with CommonDirectives with Version1JsonSupport {
  
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val timeout = api.timeout
  implicit val log = LoggerFactory.getLogger(classOf[ContributorApi])
  
  override val responseTypeHints = ExplicitTypeHints(Map(
    classOf[ListPage[_]] -> "urn:blinkboxbooks:schema:list",
    classOf[Contributor] -> "urn:blinkboxbooks:schema:contributor")
  )
  
  val idParam = "id"
  val groupParam = "groupname"

    /*
  val getContributors = pathEndOrSingleSlash {
    get {
      parameterSeq { params =>
        val idList = params.collect { case (`idParam`, value) => value }
        val groupNameList = params.collect { case (`groupParam`, value) => value }
        (idList, groupNameList) match {
          case (list, Nil) =>
            if (list.size <= config.maxResults) onSuccess(service.getContributorsById(list))(cacheable(config.maxAge, _))
            else uncacheable(BadRequest, Error("max_results_exceeded", s"Max results exceeded: ${config.maxResults}"))
          case (Nil, groupName :: Nil) =>
            paged(defaultCount = 50) { page =>
              onSuccess(service.getContributorsByGroupName(groupName, groupParam, page))(cacheable(config.maxAge, _))
            }
          case _ => uncacheable(BadRequest, None)
        }
      }
    }
  }
  * 
  */

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
  
  /*
   * 
  val getContributorGroupById = path(LongNumber) { id =>
    onSuccess(service.getGroupById(id))(cacheable(config.maxAge, _))
  }

  val routes = rootPath(config.api.localUrl.path) {
    monitor() {
      respondWithHeader(RawHeader("Vary", "Accept, Accept-Encoding")) {
        getContributorGroups ~ getContributorGroupById
      }
    }
  }
}
*/
   
}
