import AssemblyKeys._

name := "catalogue-v2"

lazy val buildSettings = Seq(
  organization := "com.blinkbox.books",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion := "2.11.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7"),
  parallelExecution in Test := false
)

lazy val common = (project in file("common")).settings(buildSettings: _*)

lazy val ingesterv1 = (project in file("ingesterv1")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*)

lazy val ingesterv2 = (project in file("ingesterv2")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*)


lazy val searchv1 = (project in file("searchv1")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*)

lazy val catalogue = (project in file("catalogue")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*)

lazy val root = (project in file(".")).
  dependsOn(ingesterv1, ingesterv2, searchv1, catalogue).
  aggregate(ingesterv1, ingesterv2, searchv1, catalogue).
  settings(buildSettings: _*).
  settings(publish := {})
