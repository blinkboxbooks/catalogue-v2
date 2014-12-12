name := "catalogue2-search-public"

libraryDependencies ++= Seq(
  "com.blinkbox.books"        %% "common-spray"       % "0.17.1",
  "com.blinkbox.books.hermes" %% "rabbitmq-ha"        % "7.1.0",
  "com.blinkbox.books"        %% "common-scala-test"  % "0.3.0" % Test,
  "io.spray"                  %% "spray-testkit"      % "1.3.2" % Test
)
