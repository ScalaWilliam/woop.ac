enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)
organization := "acleague"
name := "logservice"
version := "1.0.11"
scalaVersion := "2.11.4"
resolvers += "BaseX Maven Repository" at "http://files.basex.org/maven"
libraryDependencies ++= Seq(
  "org.syslog4j" % "syslog4j" % "0.9.30",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "org.basex" % "basex" % "7.9",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.hazelcast" % "hazelcast" % "3.3.3",
  "com.typesafe" % "config" % "1.2.1",
  "joda-time" % "joda-time" % "2.6",
  "org.joda" % "joda-convert" % "1.7"
)
mainClass in Compile := Option("acleague.app.LeagueApp")
publishArtifact in (Compile, packageBin) := false
publishArtifact in (Universal, packageZipTarball) := true
publishArtifact in (Compile, packageDoc) := false


ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"
