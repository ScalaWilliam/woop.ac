organization := "ac.woop"

version := "0.1-SNAPSHOT"

name := "ms"

scalaVersion := "2.11.5"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.1"
  Seq (
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-client"     % sprayV,
    "io.spray"            %%  "spray-routing-shapeless2" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "org.bouncycastle" % "bcprov-jdk15" % "1.46",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "commons-codec" % "commons-codec" % "1.10",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "net.java.dev.jna" % "jna" % "4.1.0",
    "org.scala-lang.modules" %% "scala-async" % "0.9.2",
    "com.chuusai" %% "shapeless" % "2.0.0",
    "com.h2database" % "h2-mvstore" % "1.4.185",
    "org.json4s" %% "json4s-jackson" % "3.2.10",
    "commons-io" % "commons-io" % "2.4"

  )
}

mainClass in (Compile, run) := Some("ac.woop.MainWebApp")

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Compile, packageDoc) := false