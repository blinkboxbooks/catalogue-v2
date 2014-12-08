package com.blinkbox.books.agora.catalogue.book

import scala.concurrent.duration.FiniteDuration
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

case class BookConfig(
  path: String,
  synopsisPathLink: String,
  maxAge: FiniteDuration,
  maxResults: Int
)

object BookConfig {
  def apply(config: Config): BookConfig = BookConfig(
    config.getString("path"),
    config.getString("synopsisLink"),
    config.getDuration("maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt("maxResults")
  )
}
