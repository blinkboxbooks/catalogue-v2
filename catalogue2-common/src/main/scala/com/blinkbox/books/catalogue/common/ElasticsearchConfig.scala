package com.blinkbox.books.catalogue.common

import com.blinkbox.books.config._
import com.typesafe.config.Config
import scala.concurrent.duration.FiniteDuration

case class ElasticsearchConfig(host: String, port: Int, clusterName: String,
                               indexName: String, timeout: FiniteDuration, httpPort: Int)

object ElasticsearchConfig {
  def apply(config: Config, prefix: String = "elasticsearch"): ElasticsearchConfig = ElasticsearchConfig(
    host = config.getString(s"$prefix.host"),
    port = config.getInt(s"$prefix.port"),
    clusterName = config.getString(s"$prefix.clusterName"),
    indexName = config.getString(s"$prefix.indexName"),
    timeout = config.getFiniteDuration(s"$prefix.timeout"),
    httpPort = config.getInt(s"$prefix.httpPort")
  )
}
