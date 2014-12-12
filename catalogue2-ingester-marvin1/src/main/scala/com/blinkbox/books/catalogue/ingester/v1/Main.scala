package com.blinkbox.books.catalogue.ingester.v1

import akka.actor.ActorSystem
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers

object Main extends App with Configuration with Loggers{
  private val actorSystem = ActorSystem("catalogue-ingester", config)
  private val supervisor = Hierarchy.start(actorSystem)

  sys.addShutdownHook{
    actorSystem.shutdown()
  }
}