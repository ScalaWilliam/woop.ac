name := "woopac"

lazy val root = (project in file(".")).aggregate(
  logservice,
  logParser,
  achievements,
  pingerservice,
  rankservice
).dependsOn(
  logservice,
  achievements,
  logParser,
  pingerservice,
  rankservice
)

lazy val logParser = Project(
  id = "log-parser",
  base = file("log-parser"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(RpmPlugin)
  .settings(
    rpmVendor := "typesafe",
    libraryDependencies += json
  )

lazy val achievements = Project(
  id = "achievements",
  base = file("achievements"))
  .settings(
    libraryDependencies += json
  )

lazy val frontend = project.enablePlugins(PlayScala, SbtWeb).dependsOn(frontendJs)

lazy val logservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)
.dependsOn(logParser)

lazy val pingerservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)

lazy val rankservice = project.enablePlugins(JavaAppPackaging, LinuxPlugin, UniversalPlugin)
.dependsOn(achievements)

lazy val frontendJs = (project in file("frontend-js")).settings(scalaVersion := "2.11.6").enablePlugins(SbtJsEngine)