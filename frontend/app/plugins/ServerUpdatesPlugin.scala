package plugins

import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.libs.concurrent.Akka
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}

class ServerUpdatesPlugin(implicit app: Application) extends Plugin {

  lazy val topic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("server-status-updates")
  import akka.actor.ActorDSL._
  lazy val currentState = scala.collection.mutable.Map.empty[String, String]
  implicit lazy val system = Akka.system
  lazy val act = actor(name="server-updates")(new Act {
    become {
      case GotUpdate(j) =>
        import org.json4s._
        import org.json4s.jackson.JsonMethods._
        import org.json4s.jackson.Serialization
        import org.json4s.jackson.Serialization.{read, write}
        val cleanerJson = parse(j) removeField {
          case ("ip", _)=> true
          case other => false
        }
        import org.json4s.JsonDSL._
        implicit val formats = Serialization.formats(NoTypeHints)
        val serverName: String = (cleanerJson \ "now" \ "server" \ "server").extract[String]
        println(serverName)
        println(j)
        val newJsonS = write(cleanerJson)
        currentState += serverName -> newJsonS
        context.system.eventStream.publish(ServerState(serverName, newJsonS))
      case GiveStates =>
        sender() ! CurrentStates(currentState.toMap)
    }
  })
  lazy val thingyId = topic.addMessageListener(new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      act ! GotUpdate(message.getMessageObject)
    }
  })
    override def onStart(): Unit = {
      thingyId
    }
  override def onStop(): Unit = {
    topic.removeMessageListener(thingyId)
  }

}
object ServerUpdatesPlugin {
  case object GiveStates
  case class GotUpdate(j: String)
  case class CurrentStates(servers: Map[String,String]) {
    def toJson = {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      import org.json4s.jackson.Serialization
      import org.json4s.jackson.Serialization.{read, write}
      implicit val formats = Serialization.formats(NoTypeHints)
      write(this.servers.map(x => x._1 -> parse(x._2)).toMap)
    }
  }
  case class ServerState(server: String, json: String)
  def serverUpdatesPlugin: ServerUpdatesPlugin = Play.current.plugin[ServerUpdatesPlugin]
    .getOrElse(throw new RuntimeException("ServerUpdatesPlugin plugin not loaded"))
}