import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.SettingsHelper._

organization := "acleague"

name:="frontend"

version := "2.01"

scalaVersion := "2.11.4"

enablePlugins(PlayScala)

enablePlugins(SbtWeb)

resolvers += "BaseX Maven Repository" at "http://files.basex.org/maven"

libraryDependencies ++= Seq(
  "org.basex" % "basex" % "7.9",
  "com.hazelcast"%"hazelcast"%"3.3.3",
  "org.scalactic" %%"scalactic"%"2.2.1",
  "com.maxmind.geoip2"%"geoip2"%"2.1.0",
  "org.scala-lang.modules" %% "scala-async" % "0.9.2",
  ws
)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

crossPaths := false

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Universal, packageZipTarball) := true

makeDeploymentSettings(Universal, packageZipTarball in Universal, "tgz")