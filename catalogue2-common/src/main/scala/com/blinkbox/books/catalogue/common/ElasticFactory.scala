package com.blinkbox.books.catalogue.common

import java.util.concurrent.ForkJoinPool
import akka.actor.ActorRefFactory
import com.blinkbox.books.elasticsearch.client.SprayElasticClient
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.ImmutableSettings
import scala.concurrent.ExecutionContext

object ElasticFactory {

  def remote(config: ElasticsearchConfig) =
    ElasticClient.remote(
      ImmutableSettings.
        builder().
        put("cluster.name", config.clusterName).
        put("transport.tcp.connect_timeout", s"${config.timeout.toSeconds}s")
        build(),
      config.host -> config.port)

  def http(config: ElasticsearchConfig)(implicit arf: ActorRefFactory) = {
    implicit val ec = DiagnosticExecutionContext(ExecutionContext.fromExecutorService(new ForkJoinPool))
    new SprayElasticClient(config.host, config.httpPort)
  }

}
