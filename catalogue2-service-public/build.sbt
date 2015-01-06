name := "catalogue2-service-public"

libraryDependencies ++= Seq(
)

javaOptions := Seq("-Duser.timezone=UTC")

fork in Test := true
