name := "catalogue-ingester"

val AkkaVersion = "2.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-kernel" % AkkaVersion,
  "com.blinkbox.books" %% "common-config" % "1.4.1",
  "com.blinkbox.books" %% "common-json" % "0.2.3",
  "com.blinkbox.books.hermes" %% "rabbitmq-ha" % "7.1.0",
  "com.blinkbox.books.hermes" %% "message-schemas" % "0.7.2",
  "com.sksamuel.elastic4s" %% "elastic4s" % "1.3.2",
  "com.blinkbox.books" %% "common-scala-test" % "0.3.0" % Test,
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test
)
