name := "rankservice"

organization := "acleague"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.apache.httpcomponents" % "httpclient" % "4.3.6",
  "org.apache.httpcomponents" % "fluent-hc" % "4.3.6",
  "com.typesafe.akka" %% "akka-actor" % "2.3.8",
  "commons-net" % "commons-net" % "3.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "com.maxmind.geoip2"%"geoip2"%"2.1.0",
  "com.hazelcast" % "hazelcast" % "3.4",
  "com.typesafe.akka"   %%  "akka-testkit"  % "2.3.8" % "test"
)