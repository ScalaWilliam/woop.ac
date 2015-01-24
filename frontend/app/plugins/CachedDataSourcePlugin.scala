package plugins

import java.util.concurrent.TimeUnit

import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.libs.concurrent.Akka
import plugins.DataSourcePlugin.UserProfile
import plugins.NewGamesPlugin.GotNewGame
import plugins.NewUserEventsPlugin.{UpdatedUserEvents, GotNewUserEvent}
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, ExecutionContext}
import scala.xml.{PCData, Text}

class CachedDataSourcePlugin(implicit app: Application) extends Plugin {
  import scala.concurrent.Future
  import akka.actor.ActorSystem
  import spray.caching.{LruCache, Cache}
  import spray.util._
  import ExecutionContext.Implicits.global
  lazy val newGamesTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("new-games")
  lazy val userUpdatesTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("user-updates")
  lazy val newUserEventsTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("user-events")
  lazy val demoDownloadTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("downloaded-demos")

  lazy val clearerListener = new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      mainCache.clear()
      userCache.clear()
      eventsCache.clear()
      refresherActor ! Refresh
    }
  }
  implicit lazy val system = Akka.system

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
  def viewUser(userId: String)(implicit ec: ExecutionContext) = {
    userCache.apply(userId, () => DataSourcePlugin.plugin.viewUser(userId))
  }
  def getGames = {
    mainCache.apply(Unit, () => DataSourcePlugin.plugin.getGames)
  }
  def getGame(id: String) = {
    mainCache.apply(id, () => DataSourcePlugin.plugin.getGame(id))
  }
  def getEvents = {
    eventsCache.apply(Unit, () => DataSourcePlugin.plugin.getEvents)
  }

  var newGamesTopicTopicListenerId: String = _
  var newUserEventsTopicListenerId: String = _
  var demoDownloadTopicListenerId: String = _
  var userUpdatesTopicListenerId: String = _
  override def onStart(): Unit = {
    newGamesTopicTopicListenerId = newGamesTopic.addMessageListener(clearerListener)
    newUserEventsTopicListenerId = newUserEventsTopic.addMessageListener(clearerListener)
    demoDownloadTopicListenerId = demoDownloadTopic.addMessageListener(clearerListener)
    userUpdatesTopicListenerId = userUpdatesTopic.addMessageListener(clearerListener)
    reloadDefault()
  }
  override def onStop(): Unit = {
    newGamesTopic.removeMessageListener(newGamesTopicTopicListenerId)
    newUserEventsTopic.removeMessageListener(newUserEventsTopicListenerId)
    demoDownloadTopic.removeMessageListener(demoDownloadTopicListenerId)
    userUpdatesTopic.removeMessageListener(userUpdatesTopicListenerId)
  }
  import akka.actor.ActorDSL._

}
object CachedDataSourcePlugin {
  def plugin: CachedDataSourcePlugin = Play.current.plugin[CachedDataSourcePlugin]
    .getOrElse(throw new RuntimeException("CachedDataSourcePlugin plugin not loaded"))
}