package acleague.actors

import acleague.publishers.GamePublisher.NewlyAdded
import akka.actor.{ActorLogging, Props}
import com.hazelcast.core.Hazelcast
import akka.actor.ActorDSL._
object HazelcastPublisherActor {
  def props(topicName: String, demoTopicName: String) = {
    Props(new HazelcastPublisherActor(topicName, demoTopicName))
  }
}
class HazelcastPublisherActor(topicName: String, demoTopicName: String) extends Act with ActorLogging {
  val hazelcast = Hazelcast.newHazelcastInstance()
  val topic = hazelcast.getTopic[String](topicName)
  val demoTopic = hazelcast.getTopic[String](demoTopicName)
  whenStarting {
    log.info("Starting hazelcast. Config: {}", hazelcast.getConfig)
    log.info("Hazelcast topic: {}", topic)
  }
  become {
    case NewlyAdded(newId) =>
      topic.publish(newId)
    case GameDemoFound(gameId, _, _) =>
      demoTopic.publish(gameId)
  }
}
