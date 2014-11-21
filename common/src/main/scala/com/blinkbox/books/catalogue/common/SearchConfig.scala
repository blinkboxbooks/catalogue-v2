package com.blinkbox.books.catalogue.common

import com.typesafe.config.Config

case class SearchConfig(host: String, port: Int, clusterName: String, indexName: String)

object SearchConfig {
  def apply(config: Config, prefix: String = "search"): SearchConfig = SearchConfig(
    host = config.getString(s"$prefix.host"),
    port = config.getInt(s"$prefix.port"),
    clusterName = config.getString(s"$prefix.clusterName"),
    indexName = config.getString(s"$prefix.indexName")
  )
}
