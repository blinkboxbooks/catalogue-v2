name := "catalogue2-ingester-marvin1"

val AkkaVersion = "2.3.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"       % AkkaVersion,
  "com.typesafe.akka"         %% "akka-kernel"      % AkkaVersion,
  "com.blinkbox.books.hermes" %% "rabbitmq-ha"      % "8.1.1",
  "com.typesafe.akka"         %% "akka-testkit"     % AkkaVersion % Test
)

fork in Test := true
