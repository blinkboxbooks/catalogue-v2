package com.blinkbox.books.agora.catalogue.app

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.{DiagnosticExecutionContext, Loggers}
import com.blinkbox.books.spray._
import spray.can.Http
import spray.http.AllOrigins
import spray.http.HttpHeaders.`Access-Control-Allow-Origin`
import spray.http.Uri.Path
import spray.routing._
import com.typesafe.config.Config
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.agora.catalogue.contributor._
import com.blinkbox.books.agora.catalogue.book._

case class AppConfig(
  api: ApiConfig,
  book: BookConfig,
  contributor: ContributorConfig
)

object AppConfig {
  def apply(config: Config): AppConfig = {
    val key = "service.catalogue.api.public"
    val cfg = config.getConfig(key)
    AppConfig(
      ApiConfig(config, key),
      BookConfig(cfg.getConfig("book")),
      ContributorConfig(cfg.getConfig("contributor"))
    )
  }
}

class WebService(config: AppConfig) extends HttpServiceActor {
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)

  /*
  val linkHelper = new LinkHelper(config.service.externalUrl, config.contributor.path,
    config.publisher.path, config.price.path, config.book.path, config.book.synopsisPathLink)

  val spring = new ClassPathXmlApplicationContext("spring/beans/service-beans.xml")
  val bookService = spring.getBean("bookService", classOf[BookService])
  val bookMediaService = spring.getBean("bookMediaService", classOf[BookMediaService])
  val pricingService = spring.getBean("pricingService", classOf[PricingService])
  val ccpCalculator = spring.getBean("clubcardPointsCalculatorService", classOf[ClubcardPointsCalculatorService])
  val publisherService = spring.getBean("publisherService", classOf[PublisherService])
  val contributorService = spring.getBean("contributorService", classOf[ContributorService])
  val categoryService = spring.getBean("categoryService", classOf[SiteCategoryService])
  val categoryMessageSource = spring.getBean("categoryMessageSource", classOf[DelegatingMessageSource])
  val siteService = spring.getBean("siteService", classOf[SiteService])
  val searchService = spring.getBean("searchService", classOf[SearchService])
  val promotionService = spring.getBean("promotionService", classOf[PromotionService])
  val genreService = spring.getBean("genreService", classOf[GenreService])

  val priceApi = new PriceApi(config.price, new PriceServiceImpl(bookService, pricingService, siteService, ccpCalculator, linkHelper))
  val synopsisApi = new SynopsisApi(config.synopsis, new SynopsisServiceImpl(bookService))
  val publisherApi = new PublisherApi(config.publisher, new PublisherServiceImpl(publisherService, linkHelper, linkHelper.makeRelative(config.publisher.api.externalUrl).toString))
  val contributorApi = new ContributorApi(config.contributor, new ContributorServiceImpl(contributorService, linkHelper, linkHelper.makeRelative(config.contributor.api.externalUrl).toString))
  val contributorGroupApi = new ContributorGroupApi(config.contributorGroup, new ContributorGroupServiceImpl(contributorService, linkHelper, linkHelper.makeRelative(config.contributorGroup.api.externalUrl).toString))
  val categoryApi = new CategoryApi(config.category, new CategoryServiceImpl(categoryService, categoryMessageSource, siteService, linkHelper, linkHelper.makeRelative(config.category.api.externalUrl).toString))
  val bookApi = new BookApi(config.book, new BookServiceImpl(bookService, bookMediaService, searchService, siteService, contributorService, publisherService, promotionService, genreService, categoryService, linkHelper, linkHelper.makeRelative(config.book.api.externalUrl).toString))
  */
  
  val bookApi = new BookApi(config.api, config.book, new ElasticSearchBookService)
  val contributorApi = new ContributorApi(config.api, config.contributor, new ElasticSearchContributorService)

  val route = respondWithHeader(`Access-Control-Allow-Origin`(AllOrigins)) {
//    priceApi.routes ~ synopsisApi.routes ~ publisherApi.routes ~ contributorApi.routes ~
//    contributorGroupApi.routes ~ categoryApi.routes ~ bookApi.routes
    bookApi.routes ~ contributorApi.routes 
  }
  
  /*
  val healthService = new HealthCheckHttpService {
    override implicit def actorRefFactory = that.actorRefFactory
    override val basePath = Path("/")
  }
  */
  
  def receive = runRoute(route)
}

object WebApp extends App with Configuration with Loggers {
  val appConfig = AppConfig(config)
  implicit val system = ActorSystem("akka-spray", config)
  implicit val executionContext = DiagnosticExecutionContext(system.dispatcher)
  implicit val timeout = Timeout(appConfig.api.timeout)
  sys.addShutdownHook(system.shutdown())
  val service = system.actorOf(Props(classOf[WebService], appConfig))
  val localUrl = appConfig.api.localUrl
  HttpServer(Http.Bind(service, localUrl.getHost, port = localUrl.effectivePort))
}
