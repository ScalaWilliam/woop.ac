organization := "acl"

name := "acleague"

version := "1.0.2"

scalaVersion := "2.11.6"

ideaExcludeFolders += ".idea"

scalacOptions += "-target:jvm-1.8"

ideaExcludeFolders += ".idea_modules"

val commonSettings = Seq(
  scalaVersion := "2.11.6",
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-async" % "0.9.3",
    "org.scalactic" %%"scalactic"%"2.2.4",
    "com.hazelcast"%"hazelcast"%"3.4.2",
    "joda-time"           % "joda-time" % "2.7",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "org.joda" % "joda-convert" % "1.7",
    "org.json4s" %% "json4s-jackson" % "3.2.11",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
  )
)

lazy val root = (project in file(".")).aggregate(acm, frontend, logservice, pingerservice, rankservice, processing, hazy)

//lazy val next = project.dependsOn(acm).settings(commonSettings :_*)

lazy val acm = project.settings(commonSettings :_*)

lazy val processing = project.settings(commonSettings :_*)

lazy val hazy = project.settings(commonSettings :_*)

lazy val frontend = project.enablePlugins(PlayScala, SbtWeb).settings(commonSettings :_*).dependsOn(processing)

lazy val logservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin).settings(commonSettings :_*)

lazy val pingerservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin).settings(commonSettings :_*)

lazy val rankservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin).settings(commonSettings :_*)