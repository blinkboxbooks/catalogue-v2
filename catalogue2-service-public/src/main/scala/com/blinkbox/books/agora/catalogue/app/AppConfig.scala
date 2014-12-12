package com.blinkbox.books.agora.catalogue.app

import java.net.{MalformedURLException, URL}
import java.util.concurrent.TimeUnit

import com.blinkbox.books.agora.catalogue.book.BookConfig
import com.blinkbox.books.agora.catalogue.contributor.ContributorConfig
import com.blinkbox.books.catalogue.common.ElasticsearchConfig
import com.blinkbox.books.config._
import com.typesafe.config.Config
import com.typesafe.config.ConfigException.BadValue

import scala.concurrent.duration._

case class AppConfig(service: ApiConfig,
                     book: BookConfig,
                     contributor: ContributorConfig,
                     publisher: PublisherConfig,
                     price: PriceConfig,
                     synopsis: SynopsisConfig,
                     elastic: ElasticsearchConfig
                     )

case class PublisherConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int)

case class PriceConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int,
                       discountScalar: Double, ukSalePriceTaxRate: Double)

case class SynopsisConfig(api: ApiConfig, path: String, maxAge: FiniteDuration, maxResults: Int)

object AppConfig {
  def apply(config: Config): AppConfig = {
    val prefix = "service.catalogue"
    val key = "service.catalogue.api.public"
    val cfg = config.getConfig(key)
    AppConfig(
      ApiConfig(config, key),
      BookConfig(cfg.getConfig("book")),
      ContributorConfig(cfg.getConfig("contributor")),
      PublisherConfig(config, s"$prefix.api.public"),
      PriceConfig(config, s"$prefix.api.public"),
      SynopsisConfig(config, s"$prefix.api.public"),
      ElasticsearchConfig(config)
    )
  }
}

object PublisherConfig {
  def apply(config: Config, prefix: String): PublisherConfig = PublisherConfig(
    ServiceConfig(config, prefix, config.getString(s"$prefix.publisher.path")),
    config.getString(s"$prefix.publisher.path"),
    config.getDuration(s"$prefix.publisher.maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt(s"$prefix.publisher.maxResults")
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
