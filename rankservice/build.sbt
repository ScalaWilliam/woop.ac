name := "rankservice"

organization := "acleague"

version := "2.05"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalactic" %% "scalactic" % "2.2.4",
  "org.apache.httpcomponents" % "httpclient" % "4.4.1",
  "org.apache.httpcomponents" % "fluent-hc" % "4.4.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.10",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.10",
  "commons-net" % "commons-net" % "3.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
  "com.maxmind.geoip2"%"geoip2"%"2.2.0",
  "com.hazelcast" % "hazelcast" % "3.4.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.akka" %% "akka-testkit"  % "2.3.10" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "joda-time" % "joda-time" % "2.7",
  "io.spray"            %%  "spray-can"     % "1.3.3",
  "io.spray"            %%  "spray-routing" % "1.3.3",
  "org.joda" % "joda-convert" % "1.7"
)

mainClass in Compile := Option("acleague.ranker.app.MasterRankerApp")

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Universal, packageZipTarball) := true

publishArtifact in (Compile, packageDoc) := false

bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts")

ideaExcludeFolders ++= Seq(".idea", ".idea_modules")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

unmanagedClasspath in Runtime += baseDirectory.value / "src/universal/conf"

Seq(com.atlassian.labs.gitstamp.GitStampPlugin.gitStampSettings: _*)