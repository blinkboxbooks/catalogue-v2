package com.blinkbox.books.agora.catalogue.app

import java.net.{MalformedURLException, URL}
import java.util.concurrent.TimeUnit

//import com.blinkbox.books.config._
import com.typesafe.config.Config
//import com.typesafe.config.ConfigException.BadValue

import scala.concurrent.duration._

/*
case class AppConfig(service: ApiConfig, db: DatabaseConfig, url: InternalUrls, price: PriceConfig, synopsis: SynopsisConfig,
                     publisher: PublisherConfig, contributor: ContributorConfig, contributorGroup: ContributorGroupConfig,
                     category: CategoryConfig, book: BookConfig, swagger: SwaggerConfig)
*/


/*
case class InternalUrls(clubcard: URL, search: URL)

case class PriceConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int,
                       discountScalar: Double, ukSalePriceTaxRate: Double)

case class SynopsisConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int)

case class PublisherConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int)
*/



/*
case class ContributorGroupConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int)

case class CategoryConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int)
*/





/*
object AppConfig {
  val prefix = "service.catalogue"
  def apply(config: Config): AppConfig = {
    val cfg = AppConfig(
      ApiConfig(config, s"$prefix.api.public"),
      DatabaseConfig(config, s"$prefix.db"),
      InternalUrls(config),
      PriceConfig(config, s"$prefix.api.public"),
      SynopsisConfig(config, s"$prefix.api.public"),
      PublisherConfig(config, s"$prefix.api.public"),
      ContributorConfig(config, s"$prefix.api.public"),
      ContributorGroupConfig(config, s"$prefix.api.public"),
      CategoryConfig(config, s"$prefix.api.public"),
      BookConfig(config, s"$prefix.api.public"),
      SwaggerConfig(config, 1)
    )
    System.setProperty(s"$prefix.db.url", cfg.db.jdbcUrl)
    System.setProperty(s"$prefix.db.user", cfg.db.user)
    System.setProperty(s"$prefix.db.pass", cfg.db.pass)
    System.setProperty(s"$prefix.api.public.pricing.discountScalar", cfg.price.discountScalar.toString)
    System.setProperty(s"$prefix.api.public.pricing.ukSalePriceTaxRate", cfg.price.ukSalePriceTaxRate.toString)
    System.setProperty("service.clubcard.api.internalUrl", cfg.url.clubcard.toString)
    System.setProperty("service.search.api.internalUrl", cfg.url.search.toString)
    cfg
  }
}
*/

/*
object InternalUrls {
  def apply(config: Config): InternalUrls = InternalUrls(
    config.getHttpUrl("service.clubcard.api.internalUrl"),
    config.getHttpUrl("service.search.api.internalUrl")
  )
}

object PriceConfig {
  def apply(config: Config, prefix: String): PriceConfig = PriceConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.pricing.path")),
    config.getString(s"$prefix.pricing.path"),
    config.getDuration(s"$prefix.pricing.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.pricing.maxResults"),
    config.getDouble(s"$prefix.pricing.discountScalar"),
    config.getDouble(s"$prefix.pricing.ukSalePriceTaxRate")
  )
}

object SynopsisConfig {
  def apply(config: Config, prefix: String): SynopsisConfig = SynopsisConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.synopsis.path")),
    config.getString(s"$prefix.synopsis.path"),
    config.getDuration(s"$prefix.synopsis.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.synopsis.maxResults")
  )
}

object PublisherConfig {
  def apply(config: Config, prefix: String): PublisherConfig = PublisherConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.publisher.path")),
    config.getString(s"$prefix.publisher.path"),
    config.getDuration(s"$prefix.publisher.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.publisher.maxResults")
  )
}

object ContributorConfig {
  def apply(config: Config, prefix: String): ContributorConfig = ContributorConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.contributor.path")),
    config.getString(s"$prefix.contributor.path"),
    config.getDuration(s"$prefix.contributor.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.contributor.maxResults")
  )
}

object ContributorGroupConfig {
  def apply(config: Config, prefix: String): ContributorGroupConfig = ContributorGroupConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.contributor-group.path")),
    config.getString(s"$prefix.contributor-group.path"),
    config.getDuration(s"$prefix.contributor-group.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.contributor-group.maxResults")
  )
}

object CategoryConfig {
  def apply(config: Config, prefix: String): CategoryConfig = CategoryConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.category.path")),
    config.getString(s"$prefix.category.path"),
    config.getDuration(s"$prefix.category.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.category.maxResults")
  )
}

object BookConfig {
  def apply(config: Config, prefix: String): BookConfig = BookConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.book.path")),
    config.getString(s"$prefix.book.path"),
    config.getString(s"$prefix.book.synopsisLink"),
    config.getDuration(s"$prefix.book.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.book.maxResults"),
    config.getInt(s"$prefix.book.maxRelatedBooks")
  )
}

object ServiceConfig {
  def apply(config: Config, prefix: String, path: String): ApiConfig = ApiConfig(
    getHttpUrl(config, s"$prefix.externalUrl", path),
    getHttpUrl(config, s"$prefix.localUrl", path),
    config.getDuration(s"$prefix.timeout", TimeUnit.MILLISECONDS).millis)

  // TODO: Move this logic to common-config
  private def getHttpUrl(config: Config, configPath: String, servicePath: String): URL = {
    val s = config.getString(configPath) + servicePath
    val url = try new URL(s) catch {
      case e: MalformedURLException => throw new BadValue(config.origin, configPath, s"Invalid URL '$s")
    }
    val schemes = List("http", "https")
    if (!schemes.contains(url.getProtocol)) throw new BadValue(config.origin, configPath, s"Invalid scheme '$url'.")
    url
  }
}
*/
