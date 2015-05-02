package plugins

import akka.actor.{Kill, ActorRef}
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Akka
import plugins.AwaitUserUpdatePlugin.{WaitForUser, UserUpdated}
import akka.pattern.ask
import concurrent.duration._
import scala.concurrent._
import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import akka.actor.ActorDSL._
import javax.inject._
@Singleton
class AwaitUserUpdatePlugin @Inject()(applicationLifecycle: ApplicationLifecycle, hazelcastPlugin: HazelcastPlugin) {
  def awaitUser(userId: String): Future[Unit] = {
    import ExecutionContext.Implicits.global
    googer.ask(WaitForUser(userId))(10.seconds).map(x => ())
  }

  lazy val topic = hazelcastPlugin.hazelcast.getTopic[String]("user-updates")

  implicit lazy val system = Akka.system(Play.current)

  var listenerId: String = _

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


  applicationLifecycle.addStopHook(() => {
    import concurrent._
    import ExecutionContext.Implicits.global
    Future(blocking {
      if (listenerId != null) {
        topic.removeMessageListener(listenerId)
      }
      googer ! Kill
    })
  })

    listenerId = topic.addMessageListener {
      new MessageListener[String] {
        override def onMessage(message: Message[String]): Unit = {
          googer ! UserUpdated(message.getMessageObject)
        }
      }
    }

}

object AwaitUserUpdatePlugin {
  case class UserUpdated(userId: String)
  case class WaitForUser(userId: String)
}