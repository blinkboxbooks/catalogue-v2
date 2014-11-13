name := "catalogue"

val AkkaVersion = "2.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"       % AkkaVersion,
  "com.typesafe.akka"         %% "akka-kernel"      % AkkaVersion,
  "com.typesafe.akka"         %% "akka-testkit"     % AkkaVersion % Test,
  "com.blinkbox.books"        %% "common-spray"     % "0.17.1"
)
