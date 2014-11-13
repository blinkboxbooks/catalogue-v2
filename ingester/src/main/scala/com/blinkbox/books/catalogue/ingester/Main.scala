package com.blinkbox.books.catalogue.ingester

import akka.actor.ActorSystem
import com.blinkbox.books.config.Configuration

object Main extends App with Configuration{
  private val actorSystem = ActorSystem("catalogue-ingester", config)
  private val supervisor = Hierarchy.start(actorSystem)
  //Hierarchy.startTestScheduler(actorSystem)

  sys.addShutdownHook{
    actorSystem.shutdown()
  }
}