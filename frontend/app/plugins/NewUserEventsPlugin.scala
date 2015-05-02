package plugins

import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Akka
import plugins.NewGamesPlugin.GotNewGame
import plugins.NewUserEventsPlugin.{UpdatedUserEvents, GotNewUserEvent}
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}
import javax.inject._
@Singleton
class NewUserEventsPlugin @Inject()(applicationLifecycle: ApplicationLifecycle, hazelcastPlugin: HazelcastPlugin, dataSourcePlugin: DataSourcePlugin) {

  lazy val topic = hazelcastPlugin.hazelcast.getTopic[String]("user-events")
  import akka.actor.ActorDSL._
  implicit lazy val system = Akka.system(Play.current)


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
            for {json <- dataSourcePlugin.getEvents} {
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
    act
    thingyId

  applicationLifecycle.addStopHook(() => {

    import concurrent._
    import ExecutionContext.Implicits.global
    Future(blocking(topic.removeMessageListener(thingyId)))

  })

}
object NewUserEventsPlugin {
  case class GotNewUserEvent(xml: String)
  case class UpdatedUserEvents(json: String)
//  case class ServerState(server: String, json: String)
}