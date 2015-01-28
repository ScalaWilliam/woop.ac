package plugins

import akka.actor.{Kill, ActorLogging}
import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.libs.concurrent.Akka
import plugins.NewGamesPlugin.{GotNewDemoFor, GotNewGame}
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}

class NewGamesPlugin(implicit app: Application) extends Plugin {

  lazy val topic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("new-games")
  lazy val demosTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("downloaded-demos")
  import akka.actor.ActorDSL._
  implicit lazy val system = Akka.system
  lazy val act = actor(name="new-games")(new Act with ActorLogging {
    // get a game --> publish in 10 seconds
    // get a game --> get a demo --> publish immediately
    whenStarting {
      import concurrent.duration._
      import context.dispatcher
      log.info("Starting new games plugin actor...")
      become(withGames(Set.empty))
    }
    case class PushGame(gameId: String)
    def withGames(games: Set[String]): Receive = {
      case haveNewGame @ GotNewGame(gameId) =>
        log.info(s"Got new game: $gameId")
        become(withGames(games + gameId))
        import concurrent.duration._
        import context.dispatcher
        context.system.scheduler.scheduleOnce(10.seconds, self, PushGame(gameId))
      case PushGame(gameId) if games contains gameId =>
        log.info(s"Pushing game now: $gameId")
        context.system.eventStream.publish(GotNewGame(gameId))
        become(withGames(games - gameId))
      case haveNewDemo @ GotNewDemoFor(gameId) if games contains gameId =>
        log.info(s"Got new demo: $gameId")
        self ! PushGame(gameId)
    }
  })
  lazy val thingyId = topic.addMessageListener(new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      act ! GotNewGame(message.getMessageObject)
    }
  })
  lazy val demosThingyId = demosTopic.addMessageListener(new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      act ! GotNewDemoFor(message.getMessageObject)
    }
  })
  override def onStart(): Unit = {
    act
    thingyId
    demosThingyId
  }
  override def onStop(): Unit = {
    topic.removeMessageListener(thingyId)
    demosTopic.removeMessageListener(demosThingyId)
    act ! Kill
  }

}
object NewGamesPlugin {
  case class GotNewGame(gameId: String)
  case class GotNewDemoFor(gameId: String)
  case class ServerState(server: String, json: String)
  def newGamesPlugin: NewGamesPlugin = Play.current.plugin[NewGamesPlugin]
    .getOrElse(throw new RuntimeException("NewGamesPlugin plugin not loaded"))
}