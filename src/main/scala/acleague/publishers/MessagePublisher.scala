package acleague.publishers

import java.util.Date

import acleague.enrichers.EnrichFoundGame
import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.ingesters.FoundGame
import org.basex.server.ClientSession


object MessagePublisher {
  case class ConnectionOptions(host: String = "127.0.0.1", port: Int = 1984, user: String = "admin", password: String = "admin", database: String)
  def apply(options: ConnectionOptions)(foundGame: FoundGame)(date: Date, server: String) = {
    publishMessage(options)(EnrichFoundGame(foundGame)(date, server))
  }
  def publishMessage(options: ConnectionOptions)(game: GameXmlReady) = {
    // no @id ==> we won't publish it!
    if ( Option(scala.xml.XML.loadString(game.xml) \@ "id").filterNot(_.isEmpty).isEmpty ) {
      throw new IllegalArgumentException(s"Received a game without ID: $game")
    }
    val session = new ClientSession(options.host, options.port, options.user, options.password)
    try {
      // push using idempotence
      val query = session.query(
        s"""
          |for $$new-game in /game[@id]
          |let $$existing-game := db:open("${options.database}")/game[@id=$$new-game/@id]
          |return if ( empty($$existing-game) ) then (db:add("${options.database}", $$new-game, "recorded"))
          |else ()
        """.stripMargin)
      try {
        // http://docs.basex.org/wiki/Server_Protocol:_Types
        query.context(game.xml, "document-node(element())")
        query.execute()
      } finally {
        query.close()
      }
    } finally {
      session.close()
    }
  }
}
