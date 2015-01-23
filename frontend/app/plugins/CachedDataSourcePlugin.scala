package plugins

import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import play.api.libs.concurrent.Akka
import plugins.NewGamesPlugin.GotNewGame
import plugins.NewUserEventsPlugin.{UpdatedUserEvents, GotNewUserEvent}
import plugins.ServerUpdatesPlugin.{GotUpdate, ServerState, CurrentStates, GiveStates}

import scala.concurrent.{Future, ExecutionContext}
import scala.xml.{PCData, Text}

class CachedDataSourcePlugin(implicit app: Application) extends Plugin {
  import scala.concurrent.Future
  import akka.actor.ActorSystem
  import spray.caching.{LruCache, Cache}
  import spray.util._
  import ExecutionContext.Implicits.global
  lazy val newGamesTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("new-games")
  lazy val newUserEventsTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("user-events")
  lazy val demoDownloadTopic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("downloaded-demos")

  lazy val clearerListener = new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      mainCache.clear()
      userCache.clear()
      eventsCache.clear()
    }
  }
  implicit lazy val system = Akka.system
  
  val userCache: Cache[Option[scala.xml.Elem]] = LruCache()
  val mainCache: Cache[String] = LruCache()
  val eventsCache: Cache[String] = LruCache()
  def viewUser(userId: String)(implicit ec: ExecutionContext) = {
    userCache.apply(userId, () => DataSourcePlugin.plugin.viewUser(userId))
  }

  def getGames = {
    mainCache.apply(Unit, () => DataSourcePlugin.plugin.getGames)
  }

  def getEvents = {
    eventsCache.apply(Unit, () => DataSourcePlugin.plugin.getEvents)
  }

  var newGamesTopicTopicListenerId: String = _
  var newUserEventsTopicListenerId: String = _
  var demoDownloadTopicListenerId: String = _
  override def onStart(): Unit = {
    newGamesTopicTopicListenerId = newGamesTopic.addMessageListener(clearerListener)
    newUserEventsTopicListenerId = newUserEventsTopic.addMessageListener(clearerListener)
    demoDownloadTopicListenerId = demoDownloadTopic.addMessageListener(clearerListener)
  }
  override def onStop(): Unit = {
    newGamesTopic.removeMessageListener(newGamesTopicTopicListenerId)
    newUserEventsTopic.removeMessageListener(newUserEventsTopicListenerId)
    demoDownloadTopic.removeMessageListener(demoDownloadTopicListenerId)
  }
  import akka.actor.ActorDSL._

}
object CachedDataSourcePlugin {
  def plugin: CachedDataSourcePlugin = Play.current.plugin[CachedDataSourcePlugin]
    .getOrElse(throw new RuntimeException("CachedDataSourcePlugin plugin not loaded"))
}