package com.blinkbox.books.catalogue.common

import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.ImmutableSettings

object ElasticFactory {

  def remote(config: ElasticsearchConfig) = {


    ElasticClient.remote(
      ImmutableSettings.
        builder().
        put("cluster.name", config.clusterName).
        put("transport.tcp.connect_timeout", s"${config.timeout.toSeconds}s")
        build(),
      config.host -> config.port)
  }
}
