package com.blinkbox.books.catalogue.searchv1

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.blinkbox.books.catalogue.common.{ElasticFactory, SearchConfig}
import com.blinkbox.books.config.{ApiConfig, Configuration}
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.spray.{HttpServer, url2uri}
import com.typesafe.scalalogging.slf4j.StrictLogging
import spray.can.Http
import spray.routing.HttpServiceActor

import scala.util.control.ControlThrowable

class RestApi(v1Api: SearchApi) extends HttpServiceActor {
  override def receive: Receive = runRoute(v1Api.routes)
}

object ApiApp extends App with Configuration with Loggers with StrictLogging {
  try {
    val prefix = "service.catalog-browser.api.public"
    val apiConfig = ApiConfig(config, prefix)
    val searchConfig = SearchConfig(config)

    implicit val actorSystem = ActorSystem("catalog-browser-system")
    implicit val executionContext = actorSystem.dispatcher
    implicit val startTimeout = Timeout(apiConfig.timeout)

    val client = ElasticFactory.remote(searchConfig)
    val searchService: V1SearchService = new EsV1SearchService(searchConfig, client)

    val v1Api = new SearchApi(apiConfig, searchService)

    val service = actorSystem.actorOf(Props(classOf[RestApi], v1Api), "catalog-browser-actor")
    val localUrl = apiConfig.localUrl
    HttpServer(Http.Bind(service, localUrl.getHost, localUrl.effectivePort))

    sys.addShutdownHook(actorSystem.shutdown())
  } catch {
    case ex: ControlThrowable => throw ex
    case ex: Throwable =>
      logger.error("Error during initialization of the service", ex)
      sys.exit(1)
  }
}
