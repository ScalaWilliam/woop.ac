package us.woop

import java.io.File
import java.net.URI
import acleague.actors.DemoDownloaderActor.DemoDownloaded
import acleague.actors.GameDemoFound
import acleague.enrichers.EnrichFoundGame
import acleague.ingesters.FoundGame
import acleague.publishers.GamePublisher.ConnectionOptions
import acleague.publishers.{DemoPublisher, GamePublisher}
import org.basex.BaseXServer
import org.basex.api.client.ClientSession
import org.basex.core.cmd._
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
class PusherSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  import acleague.app.Util.using
  val server = new BaseXServer("-p12391", "-e12392")
  val connectionOptions = ConnectionOptions(port = 12391, database = "for-testing")
  override def afterAll(): Unit = {
    server.stop()
  }
  def withSession[T](f: ClientSession => T): T = {
    using(new ClientSession("localhost", 12391, "admin", "admin")) { session =>
      f(session)
    }
  }
  "Pusher" must {
    "Push properly" in {
      withSession { session =>
        session.execute(new Check("for-testing"))
        def getCount = using(session.query("count(/game[@id])"))(_.execute).toInt
        val startCount = getCount
        GamePublisher.publishMessage(connectionOptions)(EnrichFoundGame(FoundGame.example)(new DateTime(), "test-server", 15))
        val endCount = getCount
        endCount shouldBe (startCount + 1)
      }
    }
    "Push demos properly" in {
      withSession { session =>
        session.execute(new Check("for-testing"))
        def getCount = using(session.query("count(/demo[@game-id])"))(_.execute).toInt
        val simpleDemo = GameDemoFound.example.copy(gameId = new scala.util.Random().nextInt())
        val startCount = getCount
        DemoPublisher.publishDemo(connectionOptions)(simpleDemo)
        val endCount = getCount
        endCount shouldBe (startCount + 1)
      }
    }
    "Push local demos properly" in {
      withSession { session =>
        session.execute(new Check("for-testing"))
        def getCount = using(session.query("count(/local-demo[@game-id])"))(_.execute).toInt
        val simpleDemo = DemoDownloaded(gameId = new scala.util.Random().nextInt(), new URI("abc:test"), new File("wat.dmo"))
        val startCount = getCount
        DemoPublisher.publishLocaldemo(connectionOptions)(simpleDemo)
        val endCount = getCount
        endCount shouldBe (startCount + 1)
      }
    }
  }
}
