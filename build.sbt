scalaVersion := "2.11.5"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % "test"

libraryDependencies += "org.bouncycastle" % "bcprov-jdk15" % "1.46"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "commons-codec" % "commons-codec" % "1.10"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.8"

libraryDependencies += "net.java.dev.jna" % "jna" % "4.1.0"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.1.0-RC2"
)

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.2"

