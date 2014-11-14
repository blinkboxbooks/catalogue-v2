package com.blinkbox.books.catalogue.common.search

import com.blinkbox.books.catalogue.common.Book
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import scala.concurrent.{ExecutionContext, Future}

trait Search {
  def lookup(isbn: String): Future[Option[Book]]
}

class EsSearch(config: Config, client: ElasticClient)(implicit ec: ExecutionContext) extends Search{
  import com.sksamuel.elastic4s.ElasticDsl._
  implicit val formats = org.json4s.DefaultFormats ++ com.blinkbox.books.json.DefaultFormats.customSerializers

  override def lookup(isbn: String): Future[Option[Book]] = {
    client.execute{
      get id isbn from s"${config.getString("search.index.name")}/book"
    } map { response =>
      if(response.isExists) Some(Serialization.read[Book](new String(response.getSourceAsBytes, "UTF-8")))
      else None
    }
  }
}