name := "pingerservice"

organization := "acleague"

version := "1.03"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.5" % "test",
  "org.scalactic" %% "scalactic" % "2.2.5",
  "com.typesafe.akka" %% "akka-actor" % "2.4.0",
  "commons-net" % "commons-net" % "3.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
  "com.maxmind.geoip2" % "geoip2" % "2.3.1",
  "com.hazelcast" % "hazelcast" % "3.4.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.0" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.0",
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "org.joda" % "joda-convert" % "1.8.1",
  "joda-time" % "joda-time" % "2.9"
)

mainClass in Compile := Option("acleague.pinger.PingerApp")

publishArtifact in(Compile, packageBin) := false

publishArtifact in(Universal, packageZipTarball) := true

publishArtifact in(Compile, packageDoc) := false

bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

unmanagedClasspath in Runtime += baseDirectory.value / "src/universal/conf"
