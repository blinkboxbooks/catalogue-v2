package com.blinkbox.books.catalogue.common

import java.util.concurrent.ForkJoinPool
import akka.actor.ActorRefFactory
import com.blinkbox.books.elasticsearch.client.SprayElasticClient
import com.blinkbox.books.logging.DiagnosticExecutionContext
import scala.concurrent.ExecutionContext

object ElasticFactory {

  def http(config: ElasticsearchConfig)(implicit arf: ActorRefFactory) = {
    implicit val ec = DiagnosticExecutionContext(ExecutionContext.fromExecutorService(new ForkJoinPool))
    new SprayElasticClient(config.host, config.httpPort)
  }

}
