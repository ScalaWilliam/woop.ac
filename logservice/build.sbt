
organization := "acleague"
name := "logservice"
version := "1.24"
resolvers += "BaseX Maven Repository" at "http://files.basex.org/maven"
libraryDependencies ++= Seq(
  "org.syslog4j" % "syslog4j" % "0.9.30",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-actor" % "2.3.10",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.10",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.10" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.hazelcast" % "hazelcast" % "3.4.2",
  "com.typesafe" % "config" % "1.2.1",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "org.basex" % "basex" % "8.1.1"
)

mainClass in Compile := Option("acleague.app.LeagueApp")
publishArtifact in (Compile, packageBin) := false
publishArtifact in (Universal, packageZipTarball) := true
publishArtifact in (Compile, packageDoc) := false
bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts")
libraryDependencies += "org.gnieh" % "logback-journal" % "0.1.0-SNAPSHOT"
ideaExcludeFolders ++= Seq(".idea", ".idea_modules")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
unmanagedClasspath in Runtime += baseDirectory.value / "src/universal/conf"
