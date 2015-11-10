import sbt._
import sbt.Keys._

object CommonSettingsPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def projectSettings = Seq(
    scalaVersion := "2.11.7",
    organization := "ac.woop",
    version := "4.0-SNAPSHOT",
    updateOptions := updateOptions.value.withCachedResolution(true),
    scalacOptions := Seq(
      "-unchecked", "-deprecation", "-encoding", "utf8", "-feature",
      "-language:existentials", "-language:implicitConversions",
      "-language:reflectiveCalls", "-target:jvm-1.8"
    ),
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.5" % "test",
      "org.scala-lang.modules" %% "scala-async" % "0.9.5",
      "org.scalactic" %% "scalactic" % "2.2.5",
      "com.hazelcast" % "hazelcast" % "3.4.6",
      "joda-time" % "joda-time" % "2.9",
      "org.scalatest" %% "scalatest" % "2.2.5" % "test",
      "org.joda" % "joda-convert" % "1.8.1",
      "org.json4s" %% "json4s-jackson" % "3.3.0",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
    )
  )

  object autoImport {
    val includeGitStamp = com.atlassian.labs.gitstamp.GitStampPlugin.gitStampSettings
    val dontDocument = Seq(
      publishArtifact in(Compile, packageDoc) := false,
      publishArtifact in packageDoc := false,
      sources in(Compile, doc) := Seq.empty
    )
  }

}
