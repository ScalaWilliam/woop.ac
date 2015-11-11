mainClass in Compile := Option("acleague.app.LeagueApp")
publishArtifact in (Compile, packageBin) := false
publishArtifact in (Universal, packageZipTarball) := true
publishArtifact in (Compile, packageDoc) := false
bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
unmanagedClasspath in Runtime += baseDirectory.value / "src/universal/conf"
libraryDependencies += "com.rabbitmq" % "amqp-client" % "3.5.6"