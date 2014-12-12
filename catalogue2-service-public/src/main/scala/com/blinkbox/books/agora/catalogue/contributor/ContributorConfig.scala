package com.blinkbox.books.agora.catalogue.contributor

import scala.concurrent.duration.FiniteDuration
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

case class ContributorConfig(
  path: String,
  maxAge: FiniteDuration,
  maxResults: Int
)

object ContributorConfig {
  def apply(config: Config): ContributorConfig = ContributorConfig(
    config.getString("path"),
    config.getDuration("maxAge", TimeUnit.MILLISECONDS).millis,
    config.getInt("maxResults")
  )
}
