organization := "acl"

name := "acleague"

version := "1.0.2"

scalaVersion := "2.11.6"

ideaExcludeFolders += ".idea"

scalacOptions += "-target:jvm-1.8"

ideaExcludeFolders += ".idea_modules"

val commonSettings = Seq(
  scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).aggregate(acm, frontend, logservice, pingerservice, rankservice)

//lazy val next = project.dependsOn(acm).settings(commonSettings :_*)

lazy val acm = project.settings(commonSettings :_*)

lazy val frontend = project.enablePlugins(PlayScala, SbtWeb).settings(commonSettings :_*)

lazy val logservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin).settings(commonSettings :_*)

lazy val pingerservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin).settings(commonSettings :_*)

lazy val rankservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin).settings(commonSettings :_*)