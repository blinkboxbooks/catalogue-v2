name := "catalogue-browser"

libraryDependencies ++= Seq(
  "com.blinkbox.books"        %% "common-spray"     % "0.17.1",
  "com.blinkbox.books.hermes" %% "rabbitmq-ha"      % "7.1.0",
  "com.blinkbox.books.hermes" %% "message-schemas"  % "0.7.0"
)
