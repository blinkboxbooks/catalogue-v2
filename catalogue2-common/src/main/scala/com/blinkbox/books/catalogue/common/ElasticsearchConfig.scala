package com.blinkbox.books.catalogue.common

import com.typesafe.config.Config

case class ElasticsearchConfig(host: String, port: Int, clusterName: String, indexName: String)

object ElasticsearchConfig {
  def apply(config: Config, prefix: String = "elasticsearch"): ElasticsearchConfig = ElasticsearchConfig(
    host = config.getString(s"$prefix.host"),
    port = config.getInt(s"$prefix.port"),
    clusterName = config.getString(s"$prefix.clusterName"),
    indexName = config.getString(s"$prefix.indexName")
  )
}
