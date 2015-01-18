package plugins

import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.libs.concurrent.Akka
import plugins.NewGamesPlugin.GotNewGame
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}

class NewGamesPlugin(implicit app: Application) extends Plugin {

  lazy val topic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("new-games")
  import akka.actor.ActorDSL._
  implicit lazy val system = Akka.system
  lazy val act = actor(name="new-games")(new Act {
    become {
      case haveNewGame @ GotNewGame(gameId) =>
        context.system.eventStream.publish(haveNewGame)
    }
  })
  lazy val thingyId = topic.addMessageListener(new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      act ! GotNewGame(message.getMessageObject)
    }
  })
  override def onStart(): Unit = {
    thingyId
  }
  override def onStop(): Unit = {
    topic.removeMessageListener(thingyId)
  }

}
object NewGamesPlugin {
  case class GotNewGame(gameId: String)
  case class ServerState(server: String, json: String)
  def newGamesPlugin: NewGamesPlugin = Play.current.plugin[NewGamesPlugin]
    .getOrElse(throw new RuntimeException("NewGamesPlugin plugin not loaded"))
}