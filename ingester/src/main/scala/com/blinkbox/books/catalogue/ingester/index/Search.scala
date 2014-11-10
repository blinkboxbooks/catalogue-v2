package com.blinkbox.books.catalogue.ingester.index

import com.blinkbox.books.catalogue.common.Book
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import scala.concurrent.Future

trait Search {
  def index(distributeBook: Book): Future[Unit]
}

class EsSearch(config: Config) extends Search {
  private val client = ElasticClient.remote(
    config.getString("search.host"),
    config.getInt("search.port"))

  client.execute(Schema.catalogue)

  override def index(distributeBook: Book): Future[Unit] = {
    Future.successful(())
  }
}