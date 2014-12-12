name := "catalogue2-service-public"

val AkkaVersion = "2.3.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"         % AkkaVersion,
  "com.typesafe.akka"         %% "akka-kernel"        % AkkaVersion,
  "com.typesafe.akka"         %% "akka-testkit"       % AkkaVersion     % Test
)

javaOptions := Seq("-Duser.timezone=UTC")

fork in Test := true
