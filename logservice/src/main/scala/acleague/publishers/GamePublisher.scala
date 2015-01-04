package acleague.publishers

import java.util.Date

import acleague.enrichers.EnrichFoundGame
import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.ingesters.FoundGame
import org.basex.server.ClientSession
import org.joda.time.DateTime


object GamePublisher {
  case class ConnectionOptions(host: String = "127.0.0.1", port: Int = 1984, user: String = "admin", password: String = "admin", database: String)
  def apply(options: ConnectionOptions)(foundGame: FoundGame)(date: DateTime, server: String, duration: Int) = {
    publishMessage(options)(EnrichFoundGame(foundGame)(date, server, duration))
  }
  sealed trait PushResult {
    def id: String
  }
  case class NewlyAdded(id: String) extends PushResult
  case class WasThere(id: String) extends PushResult
  def publishMessage(options: ConnectionOptions)(game: GameXmlReady): PushResult = {
    val id = Option(scala.xml.XML.loadString(game.xml) \@ "id").filterNot(_.isEmpty)
    // no @id ==> we won't publish it!
    if ( id.isEmpty ) {
      throw new IllegalArgumentException(s"Received a game without ID: $game")
    }

    val session = new ClientSession(options.host, options.port, options.user, options.password)
    try {
      def isItThere = {
        // check if it was already there
        val queryA = session.query(
          s"""
         |declare variable $$id external;
         |not(empty(db:open("${options.database}")/game[@id=$$id]))
       """.stripMargin
        )
        try {
          queryA.bind("id", id.get)
          queryA.execute() == "true"
        } finally {
          queryA.close()
        }
      }

      val wasThere = isItThere

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
        val isThere = isItThere
        if ( wasThere && isThere ) {
          WasThere(id.get)
        } else if ( isThere ) {
          NewlyAdded(id.get)
        } else {
          throw new IllegalStateException(s"Failed to add ID $id")
        }
      } finally {
        query.close()
      }
    } finally {
      session.close()
    }
  }
}
