name := "catalogue"

val AkkaVersion = "2.3.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"         % AkkaVersion,
  "com.typesafe.akka"         %% "akka-kernel"        % AkkaVersion,
  "com.typesafe.akka"         %% "akka-testkit"       % AkkaVersion     % Test,
  "io.spray"                  %% "spray-testkit"      % "1.3.2"         % Test,
  "com.blinkbox.books"        %% "common-spray"       % "0.17.5"
)

javaOptions := Seq("-Duser.timezone=UTC")

fork in Test := true
