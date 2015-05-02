organization := "acleague"

name:="frontend"

version := "2.16"

libraryDependencies ++= Seq(
  "com.hazelcast"%"hazelcast"%"3.4.2",
  "org.scalactic" %%"scalactic"%"2.2.4",
  "com.maxmind.geoip2"%"geoip2"%"2.2.0",
  "org.scala-lang.modules" %% "scala-async" % "0.9.3",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "org.joda" % "joda-convert" % "1.7",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "commons-net" % "commons-net" % "3.3",
  "io.spray" %% "spray-caching" % "1.3.3",
  "joda-time"           % "joda-time" % "2.7",
  ws
)

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

crossPaths := false

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Universal, packageZipTarball) := true

Seq(com.atlassian.labs.gitstamp.GitStampPlugin.gitStampSettings: _*)

routesGenerator := InjectedRoutesGenerator