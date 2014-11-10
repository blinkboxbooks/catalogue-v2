name := "catalogue-ingester"

val AkkaVersion = "2.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"       % AkkaVersion,
  "com.typesafe.akka"         %% "akka-kernel"      % AkkaVersion,
  "com.blinkbox.books.hermes" %% "rabbitmq-ha"      % "7.1.0",
  "com.blinkbox.books.hermes" %% "message-schemas"  % "0.7.2",
  "com.typesafe.akka"         %% "akka-testkit"     % AkkaVersion % Test
)
