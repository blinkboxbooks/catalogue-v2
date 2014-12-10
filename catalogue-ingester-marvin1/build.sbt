name := "catalogue-ingester-marvin1"

val AkkaVersion = "2.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"       % AkkaVersion,
  "com.typesafe.akka"         %% "akka-kernel"      % AkkaVersion,
  "com.blinkbox.books.hermes" %% "rabbitmq-ha"      % "7.1.0",
  "com.typesafe.akka"         %% "akka-testkit"     % AkkaVersion % Test
)

fork in Test := true
