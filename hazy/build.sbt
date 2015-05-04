name := "hazy"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC2"
libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-RC2"
libraryDependencies += "com.typesafe.akka" %% "akka-http-scala-experimental" % "1.0-RC2"

resolvers += "basex" at "http://files.basex.org/maven"

libraryDependencies ++= Seq(
  "org.basex" % "basex" % "8.1.1",
  "org.basex" % "basex-api" % "8.1.1" exclude("com.ettrema", "milton-api"),
  "org.apache.httpcomponents" % "httpclient" % "4.4.1",
  "org.apache.httpcomponents" % "fluent-hc" % "4.4.1",
  "org.json4s" %% "json4s-jackson" % "3.2.11"
)
