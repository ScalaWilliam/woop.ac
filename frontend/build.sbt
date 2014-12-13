organization := "acleague"
name:="frontend"
version := "1.0.2"
scalaVersion := "2.11.4"
enablePlugins(PlayScala)
resolvers += "BaseX Maven Repository" at "http://files.basex.org/maven"
libraryDependencies ++= Seq(
  "org.basex" % "basex" % "7.9"
)
