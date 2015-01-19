package plugins

import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.libs.concurrent.Akka
import plugins.NewGamesPlugin.GotNewGame
import plugins.NewUserEventsPlugin.{UpdatedUserEvents, GotNewUserEvent}
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}

class NewUserEventsPlugin(implicit app: Application) extends Plugin {

  lazy val topic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("user-events")
  import akka.actor.ActorDSL._
  implicit lazy val system = Akka.system

  def getEvents = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for { r <- BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[
declare option output:method 'json';
let $events :=
let $now := current-dateTime()
for $event in /user-event[@at-game]
let $user-id := data($event/@user-id)
let $user := /registered-user[@id = $user-id]
let $name := data($user/@game-nickname)
let $game-id := data($event/@at-game)
let $game := /game[@id=$game-id]
let $date := data($game/@date)
let $dateTime := xs:dateTime($date)
let $ago := xs:dayTimeDuration($now - $dateTime)
let $when :=
  if ( $ago le xs:dayTimeDuration("PT1H") ) then ("just now")
  else if ( $ago le xs:dayTimeDuration("PT12H"))  then ("today")
  else if ( $ago le xs:dayTimeDuration("P2D")) then ("yesterday")
  else (days-from-duration($ago)||" days ago")
let $title :=
  if ( $event/became-capture-master-event ) then ("became Capture Master")
  else if ( $event/slaughtered-event ) then ("achieved Slaughterer")
  else if ( $event/solo-flagger-event ) then ("achieved Solo Flagger")
  else if ( $event/terrible-game ) then ("had a Terrible Game")
  else if ( $event/achieved-dday-event ) then ("had a D-Day")
  else if ( $event/became-flag-master ) then ("became Flag Master")
  else if ( $event/became-frag-master ) then ("became Frag Master")
  else if ( $event/became-cube-addict ) then ("became Cube Addict")
  else if ( $event/became-tdm-lover ) then ("became TDM Lover")
  else if ( $event/became-tosok-lover ) then ("became TOSOK Lover")
  else if ( $event/completed-map-event ) then ("completed map "||data($event/completed-map-event/@map))
  else if ( $event/achieved-new-flag-master-level ) then ("achieved Flag Master level "||data($event//@new-level-flags))
  else if ( $event/achieved-new-frag-master-level ) then ("achieved Frag Master level "||data($event//@new-level-frags))
  else if ( $event/achieved-new-cube-addict-level ) then ("achieved Cube Addict level "||data($event//@new-level-hours)||"h")
  else ("?? "||node-name($event/*))
let $text := $name || "  "||$title
where not(empty($text))
order by $date descending
return map { "when": $when, "user": $user-id, "title": $text }

return array { $events[position() = 1 to 7] }
]]>
      </rest:text>
      <rest:variable name="user-id" value="drakas"/>
    </rest:query>) }
    yield r.body
  }

  lazy val act = actor(name="new-user-events")(new Act {
    case object DumpEvents
    /** If we get one user update, there's a chance that we'll get many many - if ranker is re-run. **/
    /** So we wait 2s before we send everything out. **/
    becomeStacked {
      case GotNewUserEvent(_) =>
        import concurrent.duration._
        context.system.scheduler.scheduleOnce(2.seconds, self, DumpEvents)(context.dispatcher)
        becomeStacked {
          case DumpEvents =>
            unbecome()
            import scala.concurrent.ExecutionContext.Implicits.global
            for {json <- getEvents} {
              context.system.eventStream.publish(UpdatedUserEvents(json))
            }
        }
    }
  })
  lazy val thingyId = topic.addMessageListener(new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      act ! GotNewUserEvent(message.getMessageObject)
    }
  })
  override def onStart(): Unit = {
    act
    thingyId
  }
  override def onStop(): Unit = {
    topic.removeMessageListener(thingyId)
  }

}
object NewUserEventsPlugin {
  case class GotNewUserEvent(xml: String)
  case class UpdatedUserEvents(json: String)
//  case class ServerState(server: String, json: String)
  def newUserEventsPlugin: NewUserEventsPlugin = Play.current.plugin[NewUserEventsPlugin]
    .getOrElse(throw new RuntimeException("NewUserEventsPlugin plugin not loaded"))
}