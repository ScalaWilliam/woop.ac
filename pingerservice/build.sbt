enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)

name := "pingerservice"

organization := "acleague"

version := "1.00"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalactic" %% "scalactic" % "2.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.8",
  "commons-net" % "commons-net" % "3.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "com.maxmind.geoip2"%"geoip2"%"2.1.0",
  "com.hazelcast" % "hazelcast" % "3.3.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.akka" %% "akka-testkit"  % "2.3.8" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.8",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "org.joda" % "joda-convert" % "1.7",
  "joda-time" % "joda-time" % "2.4"
)

mainClass in Compile := Option("acleague.pinger.PingerApp")

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Universal, packageZipTarball) := true

publishArtifact in (Compile, packageDoc) := false

bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts")

ideaExcludeFolders ++= Seq(".idea", ".idea_modules")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

unmanagedClasspath in Runtime += baseDirectory.value / "src/universal/conf"
