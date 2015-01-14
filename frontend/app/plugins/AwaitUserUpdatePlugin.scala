package plugins

import akka.actor.{Kill, ActorRef}
import play.api.libs.concurrent.Akka
import plugins.AwaitUserUpdatePlugin.{WaitForUser, UserUpdated}
import akka.pattern.ask
import concurrent.duration._
import scala.concurrent._
import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import akka.actor.ActorDSL._

class AwaitUserUpdatePlugin(implicit app: Application) extends Plugin {
  def awaitUser(userId: String): Future[Unit] = {
    import ExecutionContext.Implicits.global
    googer.ask(WaitForUser(userId))(10.seconds).map(x => ())
  }
  implicit lazy val system = Akka.system
  lazy val listener = new MessageListener[String] {
    override def onMessage(message: Message[String]): Unit = {
      googer ! UserUpdated(message.getMessageObject)
    }
  }
  lazy val topic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("user-updates")
  lazy val listenerId = topic.addMessageListener(listener)
  lazy val googer = actor(new Act {
    val waiters = scala.collection.mutable.Map.empty[ActorRef, String]
    become {
      case WaitForUser(userId) =>
        waiters += sender() -> userId
      case UserUpdated(uname) =>
        for {
          (actor, `uname`) <- waiters
        } {
          actor ! true
          waiters.remove(actor)
        }
    }
  })

  override def onStop(): Unit = {
    topic.removeMessageListener(listenerId)
    googer ! Kill
  }

}

object AwaitUserUpdatePlugin {
  case class UserUpdated(userId: String)
  case class WaitForUser(userId: String)
  def awaitPlugin: AwaitUserUpdatePlugin = Play.current.plugin[AwaitUserUpdatePlugin]
    .getOrElse(throw new RuntimeException("AwaitUserUpdatePlugin plugin not loaded"))
}