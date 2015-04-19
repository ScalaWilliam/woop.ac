package acleague.ranker.app

import acleague.ranker.actors.RankerSecond.{FoundEvent, UpdatedUser, NewGameFound}
import acleague.ranker.actors.{MasterRanker, RankerActor}
import akka.actor.ActorDSL._
import akka.actor.{Kill, ActorLogging, ActorSystem, Terminated}
import com.hazelcast.core.{Hazelcast, Message, MessageListener}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/**
 * Created by William on 18/01/2015.
 */
object MasterRankerApp extends App with LazyLogging with HttpEndpoint {
  System.setProperty("hazelcast.logging.type", System.getProperty("hazelcast.logging.type", "slf4j"))
  logger.info(s"Application configuration: ${AppConfig.conf}")
  implicit lazy val as = ActorSystem("MR")
  val woot = actor(new Act with ActWithStash with ActorLogging {

    var masterRanker = context.actorOf(MasterRanker.props)
    case object ReloadMasterRanker

    val hazelcast = Hazelcast.newHazelcastInstance()
    case class NewGameIdReceived(gameId: String)
    case class NewUserRegistered(userId: String)

    hazelcast.getTopic[String](AppConfig.hazelcastGameTopicName).addMessageListener(new MessageListener[String] {
      override def onMessage(message: Message[String]): Unit = {
        self ! NewGameIdReceived(message.getMessageObject)
      }
    })

    hazelcast.getTopic[String](AppConfig.hazelcastUserTopicName).addMessageListener(new MessageListener[String] {
      override def onMessage(message: Message[String]): Unit = {
        self ! NewUserRegistered(message.getMessageObject)
      }
    })

    become {
      case ReloadMasterRanker =>
        becomeStacked {
          case Terminated(subj) if masterRanker == subj =>
            masterRanker = context.actorOf(MasterRanker.props)
            context.unwatch(subj)
            context.watch(masterRanker)
            unbecome()
            unstashAll()
          case other => stash()
        }
        masterRanker ! Kill
      case NewUserRegistered(userId) =>
        self ! ReloadMasterRanker
      case NewGameIdReceived(gameId) =>
        import akka.pattern.pipe
        import context.dispatcher
        Future {
          for {
            newGame <- RankerActor.getGame(gameId)
          } {
            masterRanker ! NewGameFound(newGame)
          }
        } pipeTo self
      case uu: UpdatedUser =>
        hazelcast.getTopic[String](AppConfig.hazelcastUserUpdateTopicName).publish(uu.userId)
      case fe: FoundEvent =>
        hazelcast.getTopic[String](AppConfig.hazelcastUserEventTopicName).publish(fe.toXml.toString)
    }

    whenStarting {
      context.watch(masterRanker)
    }
  })
  as.awaitTermination()
  as.shutdown()
}
