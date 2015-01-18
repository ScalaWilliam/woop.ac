enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)

name := "rankservice"

organization := "acleague"

version := "1.03"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalactic" %% "scalactic" % "2.2.1",
  "org.apache.httpcomponents" % "httpclient" % "4.3.6",
  "org.apache.httpcomponents" % "fluent-hc" % "4.3.6",
  "com.typesafe.akka" %% "akka-actor" % "2.3.8",
  "commons-net" % "commons-net" % "3.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "com.maxmind.geoip2"%"geoip2"%"2.1.0",
  "com.hazelcast" % "hazelcast" % "3.3.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.akka" %% "akka-testkit"  % "2.3.8" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.8"
)

mainClass in Compile := Option("acleague.actors.MasterRankerApp")

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Universal, packageZipTarball) := true

publishArtifact in (Compile, packageDoc) := false

bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts")

ideaExcludeFolders ++= Seq(".idea", ".idea_modules")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

unmanagedClasspath in Runtime += baseDirectory.value / "src/universal/conf"
