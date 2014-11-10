import AssemblyKeys._

name := "catalogue-v2"

lazy val buildSettings = Seq(
  organization := "com.blinkbox.books",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion := "2.10.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7")
)

lazy val common = (project in file("common")).settings(buildSettings: _*)

lazy val ingester = (project in file("ingester")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*)

lazy val browser = (project in file("browser")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*)

lazy val root = (project in file(".")).
  dependsOn(ingester, browser, common).aggregate(ingester, browser, common).
  settings(publish := {})

