enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)

name := "acleague"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies += "org.syslog4j" % "syslog4j" % "0.9.30"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

//libraryDependencies += "com.sksamuel.elastic4s" %% "elastic4s" % "1.4.0"

libraryDependencies += "org.basex" % "basex" % "7.9"

resolvers += "BaseX Maven Repository" at "http://files.basex.org/maven"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test"

mainClass in Compile := Option("us.woop.EverythingIntegrated")

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Universal, packageZipTarball) := true

publishArtifact in (Compile, packageDoc) := false

