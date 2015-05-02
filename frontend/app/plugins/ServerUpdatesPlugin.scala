package plugins

import akka.util.Timeout
import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import org.json4s.jackson.Serialization._
import play.api._
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Akka
import plugins.ServerUpdatesPlugin._

import javax.inject._

import scala.concurrent.Future

@Singleton
class ServerUpdatesPlugin @Inject()(applicationLifecycle: ApplicationLifecycle, hazelcastPlugin: HazelcastPlugin, basexProviderPlugin: BasexProviderPlugin)  {

  lazy val topic = hazelcastPlugin.hazelcast.getTopic[String]("server-status-updates")
  import akka.actor.ActorDSL._
  lazy val currentState = scala.collection.mutable.Map.empty[String, String]
  var currentStateJson: String = "[]"
  implicit lazy val system = Akka.system(Play.current)

  def enrichUsers(json: String): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    basexProviderPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[
declare option output:method 'json';
declare variable $json as xs:string external;

let $jenny :=
  copy $j := json:parse($json)
  modify (
    for $player in $j//players/_
    return (
      for $ip in $player/ip
      return delete node $ip,
      for $name-node in $player/name
      let $name := data($name-node)
      for $user in /registered-user[@game-nickname = $name]
      return insert node <user>{data($user/@id)}</user> into $player
    )
  )
  return $j
return $jenny
]]>
      </rest:text>
      <rest:variable name="json" value={json}/>
    </rest:query>).map(_.body)
  }

  lazy val act = actor(name="server-updates")(new Act {
    case class EnrichedJson(serverName: String, data: String)
    import scala.concurrent.ExecutionContext.Implicits.global
    become {
      case EnrichedJson(serverName, data) =>
        currentState += serverName -> data
        currentStateJson = {
          import play.api.libs.json.{JsNull, Json, JsString, JsValue}
          val json = Json.toJson(currentState.valuesIterator.toList)
          json.toString()
        }
        context.system.eventStream.publish(ServerState(serverName, data))
      case GotUpdate(j) =>
        import org.json4s._
        import org.json4s.jackson.JsonMethods._
        import org.json4s.jackson.Serialization
        import org.json4s.jackson.Serialization.write
        implicit val formats = Serialization.formats(NoTypeHints)
        val cleanerJson = parse(j)
        val serverName: String = (cleanerJson \ "now" \ "server" \ "server").extract[String]
        import akka.pattern.pipe
        enrichUsers(json = write(cleanerJson)).map(EnrichedJson(serverName, _)) pipeTo self
      case GiveStates =>
        sender() ! CurrentStates(currentState.toMap)
      case GiveStatesJson =>
        sender() ! CurrentStatesJson(currentStateJson)
    }
  })

  def getCurrentStates: Future[CurrentStates] = {
    import concurrent.duration._
    implicit val timeout = Timeout(5.seconds)
    import akka.pattern.ask
    ask(act, GiveStates).mapTo[CurrentStates]
  }
  def getCurrentStatesJson: Future[CurrentStatesJson] = {
    import concurrent.duration._
    implicit val timeout = Timeout(5.seconds)
    import akka.pattern.ask
    ask(act, GiveStatesJson).mapTo[CurrentStatesJson]
  }
  lazy val thingyId = topic.addMessageListener(new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      act ! GotUpdate(message.getMessageObject)
    }
  })
      thingyId

  applicationLifecycle.addStopHook(() => {
    import concurrent._
    import ExecutionContext.Implicits.global
    Future(blocking(topic.removeMessageListener(thingyId)))
  })

}
object ServerUpdatesPlugin {
  case object GiveStates
  case object GiveStatesJson
  case class GotUpdate(j: String)
  case class CurrentStatesJson(j: String)
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
}