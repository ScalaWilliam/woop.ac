organization := "acleague"
name:="frontend"
version := "1.0.5"
scalaVersion := "2.11.4"
enablePlugins(PlayScala)
enablePlugins(SbtWeb)
resolvers += "BaseX Maven Repository" at "http://files.basex.org/maven"
libraryDependencies ++= Seq(
  "org.basex" % "basex" % "7.9"
)
includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
