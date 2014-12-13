package acleague.actors

import acleague.publishers.MessagePublisher.NewlyAdded
import akka.actor.{ActorLogging, Props}
import com.hazelcast.core.Hazelcast
import akka.actor.ActorDSL._
object HazelcastPublisherActor {
  def props(topicName: String) = Props(new HazelcastPublisherActor(topicName))
}
class HazelcastPublisherActor(topicName: String) extends Act with ActorLogging {
  val hazelcast = Hazelcast.newHazelcastInstance()
  val topic = hazelcast.getTopic[String](topicName)
  whenStarting {
    log.info("Starting hazelcast. Config: {}", hazelcast.getConfig)
    log.info("Hazelcast topic: {}", topic)
  }
  become {
    case NewlyAdded(newId) =>
      topic.publish(newId)
  }
}
