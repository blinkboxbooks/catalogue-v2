import AssemblyKeys._

name := "catalogue-v2"

lazy val buildSettings = Seq(
  organization := "com.blinkbox.books",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion := "2.11.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7"),
  parallelExecution in Test := false
)

lazy val artifactSettings = addArtifact(artifact in (Compile, assembly), assembly).
  settings

lazy val `catalogue2-common` = (project in file("catalogue2-common")).
  settings(buildSettings: _*)

lazy val `catalogue2-ingester-marvin1` = (project in file("catalogue2-ingester-marvin1")).
  dependsOn(`catalogue2-common` % "compile->compile;test->test").aggregate(`catalogue2-common`).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val `catalogue2-ingester-marvin2` = (project in file("catalogue2-ingester-marvin2")).
  dependsOn(`catalogue2-common` % "compile->compile;test->test").aggregate(`catalogue2-common`).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val `catalogue2-search-public` = (project in file("catalogue2-search-public")).
  dependsOn(`catalogue2-common` % "compile->compile;test->test").aggregate(`catalogue2-common`).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val `catalogue2-service-public` = (project in file("catalogue2-service-public")).
  dependsOn(`catalogue2-common` % "compile->compile;test->test").aggregate(`catalogue2-common`).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val root = (project in file(".")).
  dependsOn(`catalogue2-ingester-marvin1`, `catalogue2-ingester-marvin2`, `catalogue2-search-public`, `catalogue2-service-public`).
  aggregate(`catalogue2-ingester-marvin1`, `catalogue2-ingester-marvin2`, `catalogue2-search-public`, `catalogue2-service-public`).
  settings(buildSettings: _*).
  settings(publish := {})
