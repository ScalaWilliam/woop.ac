package acleague.publishers

import java.util.Date
import acleague.actors.DemoDownloaderActor.DemoDownloaded
import acleague.actors.GameDemoFound
import acleague.publishers.GamePublisher.ConnectionOptions
import org.basex.server.ClientSession


object DemoPublisher {
  def apply(options: ConnectionOptions)(demo: GameDemoFound) = {
    publishDemo(options)(demo)
  }
  def publishLocaldemo(options: ConnectionOptions)(downloaded: DemoDownloaded): Unit = {
    val demoXml = <local-demo game-id={downloaded.gameId} from={downloaded.source.toString} to={downloaded.destination.toString}/>

    val session = new ClientSession(options.host, options.port, options.user, options.password)
    try {

      // push using idempotence
      val query = session.query(
        s"""
          |for $$new-demo in /local-demo[@game-id]
          |let $$existing-demo := db:open("${options.database}")/local-demo[@game-id=$$new-demo/@game-id]
          |return if ( empty($$existing-demo) ) then (db:add("${options.database}", $$new-demo, "local-demos"))
          |else ()
        """.stripMargin)
      try {
        // http://docs.basex.org/wiki/Server_Protocol:_Types
        query.context(s"$demoXml", "document-node(element())")
        query.execute()
      } finally {
        query.close()
      }
    } finally {
      session.close()
    }
  }
  def publishDemo(options: ConnectionOptions)(demo: GameDemoFound): Unit = {
    val demoXml = <demo
      game-id={demo.gameId}
      date={demo.demoRecorded.dateTime}
      map={demo.demoRecorded.map}
      mode={demo.demoRecorded.mode}
      size-a={demo.demoRecorded.size}
      filename={demo.demoWritten.filename}
      size-b={demo.demoWritten.size}
    />

    val session = new ClientSession(options.host, options.port, options.user, options.password)
    try {

      // push using idempotence
      val query = session.query(
        s"""
          |for $$new-demo in /demo[@game-id]
          |let $$existing-demo := db:open("${options.database}")/demo[@game-id=$$new-demo/@game-id]
          |return if ( empty($$existing-demo) ) then (db:add("${options.database}", $$new-demo, "demos"))
          |else ()
        """.stripMargin)
      try {
        // http://docs.basex.org/wiki/Server_Protocol:_Types
        query.context(s"$demoXml", "document-node(element())")
        query.execute()
      } finally {
        query.close()
      }
    } finally {
      session.close()
    }
  }
}
