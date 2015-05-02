package plugins

import java.util.concurrent.TimeUnit

import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Akka
import plugins.DataSourcePlugin.UserProfile
import plugins.NewGamesPlugin.GotNewGame
import plugins.NewUserEventsPlugin.{UpdatedUserEvents, GotNewUserEvent}
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, ExecutionContext}
import scala.xml.{PCData, Text}

import javax.inject._

@Singleton
class CachedDataSourcePlugin @Inject()(hazelcastPlugin: HazelcastPlugin, applicationLifecycle: ApplicationLifecycle, dataSourcePlugin: DataSourcePlugin) extends DataSourcePluginInterface {
  import Play.current
  import scala.concurrent.Future
  import akka.actor.ActorSystem
  import spray.caching.{LruCache, Cache}
  import spray.util._
  import ExecutionContext.Implicits.global
  lazy val newGamesTopic = hazelcastPlugin.hazelcast.getTopic[String]("new-games")
  lazy val userUpdatesTopic = hazelcastPlugin.hazelcast.getTopic[String]("user-updates")
  lazy val newUserEventsTopic = hazelcastPlugin.hazelcast.getTopic[String]("user-events")
  lazy val demoDownloadTopic = hazelcastPlugin.hazelcast.getTopic[String]("downloaded-demos")

  lazy val clearerListener = new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      mainCache.clear()
      userCache.clear()
      eventsCache.clear()
      refresherActor ! Refresh
    }
  }
  implicit lazy val system = Akka.system(Play.current)

  import akka.actor.ActorDSL._
  case object Refresh
  lazy val refresherActor = actor(new Act {
    case object Go
    becomeStacked {
      case Refresh =>
        import concurrent.duration._
        val schedule = context.system.scheduler.scheduleOnce(5.seconds, self, Go)
        becomeStacked {
          case Refresh =>
            schedule.cancel()
            unbecome()
            self ! Refresh
          case Go =>
            unbecome()
            reloadDefault()
        }
    }
  })

  def reloadDefault(): Unit = {
    getGames
    getEvents
    viewUser("Drakas")
  }
  
  val userCache: Cache[Option[UserProfile]] = LruCache(timeToLive = Duration(1, TimeUnit.HOURS))
  val mainCache: Cache[String] = LruCache(timeToLive = Duration(1, TimeUnit.HOURS))
  val eventsCache: Cache[String] = LruCache(timeToLive = Duration(1, TimeUnit.HOURS))
  def viewUser(userId: String) = {
    userCache.apply(userId, () => dataSourcePlugin.viewUser(userId))
  }
  def getGames = {
    mainCache.apply(Unit, () => dataSourcePlugin.getGames)
  }
  def getGame(id: String) = {
    mainCache.apply(id, () => dataSourcePlugin.getGame(id))
  }
  def getEvents = {
    eventsCache.apply(Unit, () => dataSourcePlugin.getEvents)
  }

  var newGamesTopicTopicListenerId: String = _
  var newUserEventsTopicListenerId: String = _
  var demoDownloadTopicListenerId: String = _
  var userUpdatesTopicListenerId: String = _
    newGamesTopicTopicListenerId = newGamesTopic.addMessageListener(clearerListener)
    newUserEventsTopicListenerId = newUserEventsTopic.addMessageListener(clearerListener)
    demoDownloadTopicListenerId = demoDownloadTopic.addMessageListener(clearerListener)
    userUpdatesTopicListenerId = userUpdatesTopic.addMessageListener(clearerListener)
    reloadDefault()

  import concurrent.blocking
  applicationLifecycle.addStopHook(() => Future{blocking{
    newGamesTopic.removeMessageListener(newGamesTopicTopicListenerId)
    newUserEventsTopic.removeMessageListener(newUserEventsTopicListenerId)
    demoDownloadTopic.removeMessageListener(demoDownloadTopicListenerId)
    userUpdatesTopic.removeMessageListener(userUpdatesTopicListenerId)
  }  })

    import akka.actor.ActorDSL._

}
object CachedDataSourcePlugin {
}