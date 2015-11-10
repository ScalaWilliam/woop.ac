mainClass in Compile := Option("acleague.ranker.app.MasterRankerApp")

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Universal, packageZipTarball) := true

publishArtifact in (Compile, packageDoc) := false

bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts")

unmanagedClasspath in Runtime += baseDirectory.value / "src/universal/conf"

Seq(com.atlassian.labs.gitstamp.GitStampPlugin.gitStampSettings: _*)