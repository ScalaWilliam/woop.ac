package us.woop

import java.util.{Date, UUID}
import acleague.actors.{IndividualServerActor, MessagePublisherActor}
import acleague.app.Util
import acleague.enrichers.EnrichFoundGame
import acleague.ingesters.{FoundGame, GameFinishedHeader}
import acleague.publishers.MessagePublisher
import acleague.publishers.MessagePublisher.ConnectionOptions
import org.basex.BaseXServer
import org.basex.core.cmd._
import org.basex.server.ClientSession
import org.scalatest.{Matchers, WordSpec}
class PusherSpec extends WordSpec with Matchers {
  import Util.using
  "Pusher" must {
    "Push properly" in {
      val server = new BaseXServer("-p12391")
      try {
        val connectionOptions = ConnectionOptions(port = 12391, database = "for-testing")
        using(new ClientSession("localhost", 12391, "admin", "admin")) { session =>
          session.execute(new Check("for-testing"))
          def getCount = using(session.query("count(//game[@id])"))(_.execute).toInt
          val startCount = getCount
          MessagePublisher.publishMessage(connectionOptions)(EnrichFoundGame(FoundGame.example)(new Date, "test-server"))
          val endCount = getCount
          endCount shouldBe (startCount + 1)
        }
      } finally {
        server.stop()
      }
    }
  }
}
