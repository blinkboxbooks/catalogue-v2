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

lazy val common = (project in file("common")).
  settings(buildSettings: _*)

lazy val `catalogue-ingester-marvin1` = (project in file("catalogue-ingester-marvin1")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val `catalogue-ingester-marvin2` = (project in file("catalogue-ingester-marvin2")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val `catalogue-search-service` = (project in file("catalogue-search-service")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val catalogue = (project in file("catalogue")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val root = (project in file(".")).
  dependsOn(`catalogue-ingester-marvin1`, `catalogue-ingester-marvin2`, `catalogue-search-service`, catalogue).
  aggregate(`catalogue-ingester-marvin1`, `catalogue-ingester-marvin2`, `catalogue-search-service`, catalogue).
  settings(buildSettings: _*).
  settings(publish := {})
