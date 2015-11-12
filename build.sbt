name := "woopac"

lazy val root = (project in file(".")).aggregate(
  logservice,
  logParser,
  achievements,
  pingerservice,
  rankservice,
  api
).dependsOn(
  logservice,
  achievements,
  logParser,
  pingerservice,
  rankservice,
  api
)

lazy val logParser = Project(
  id = "log-parser",
  base = file("log-parser"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(RpmPlugin)
  .settings(
    rpmVendor := "typesafe",
    libraryDependencies += json,
    rpmBrpJavaRepackJars := true,
    version := "4.0",
    rpmLicense := Some("BSD")
  )

lazy val achievements = Project(
  id = "achievements",
  base = file("achievements"))
  .settings(
    libraryDependencies ++= Seq(
      json
    ),
    resolvers += "bintray-jcenter" at "http://jcenter.bintray.com/"
  ).dependsOn(logParser)

lazy val frontend = project
  .enablePlugins(PlayScala, SbtWeb)
  .dependsOn(frontendJs)

lazy val logservice = project
  .enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)
  .dependsOn(logParser)

lazy val pingerservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)

lazy val rankservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)
  .dependsOn(achievements)

lazy val frontendJs = (project in file("frontend-js")).settings(scalaVersion := "2.11.6").enablePlugins(SbtJsEngine)

lazy val api = project.enablePlugins(PlayScala).dependsOn(achievements)