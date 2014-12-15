organization := "acleague"
name:="root"
version := "1.0.2"
scalaVersion := "2.11.4"
//lazy val root = project.in( file(".") )
//  .aggregate(frontend, logservice)
//  .dependsOn(frontend, logservice)

//lazy val logservice = (project in file("logservice") enablePlugins (JavaAppPackaging, LinuxPlugin, UniversalPlugin))

//lazy val frontend = (project in file("frontend") enablePlugins PlayScala)

//resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"
