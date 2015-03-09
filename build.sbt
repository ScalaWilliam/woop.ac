organization := "acl"

name := "root"

version := "1.0.2"

scalaVersion := "2.11.6"

ideaExcludeFolders += ".idea"

scalacOptions += "-target:jvm-1.8"

ideaExcludeFolders += ".idea_modules"

lazy val root = (project in file(".")).aggregate(acm, next)

lazy val next = (project in file("next")).dependsOn(acm)

lazy val acm = project in file("acm")