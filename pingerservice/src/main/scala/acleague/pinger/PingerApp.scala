package acleague.pinger

import java.util.concurrent.TimeUnit
import acleague.pinger.Pinger.{CurrentGameStatus, ServerStatus, SendPings}
import akka.actor.ActorSystem
import com.hazelcast.core.Hazelcast
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import concurrent.duration._
object AppConfig {
  val conf = ConfigFactory.load()
  val hazelcastUpdatesTopicName = conf.getString("acleague.hazelcast.servers.topic")
  val hazelcastUpdatesMapName = conf.getString("acleague.hazelcast.servers.map")
  val updateFrequency = conf.getDuration("acleague.update-frequency", TimeUnit.SECONDS).seconds
  val serversList = {
    import collection.JavaConverters._
    val regex = """(.*):(\d+)""".r
    for {
      regex(ip, port) <- conf.getStringList("acleague.servers.list").asScala
    } yield ip -> port.toInt
  }.toSet
}
object PingerApp extends App with LazyLogging {
  System.setProperty("hazelcast.logging.type", System.getProperty("hazelcast.logging.type", "slf4j"))
  logger.info(s"Application configuration: ${AppConfig.conf}")
  implicit val system = ActorSystem("pinger-system")
  import system.dispatcher
  val pinger = system.actorOf(Pinger.props, name="pinger-actor")

  for {
    (ip, port) <- AppConfig.serversList
  } {
    system.scheduler.schedule(1.second, AppConfig.updateFrequency, pinger, SendPings(ip, port))
  }

  import akka.actor.ActorDSL._
  val hazelcast = Hazelcast.newHazelcastInstance()
  val publisherActor = actor(new Act {
    val hazelcastTopic = hazelcast.getTopic[String](AppConfig.hazelcastUpdatesTopicName)
    val hazelcastMap = hazelcast.getMap[String, String](AppConfig.hazelcastUpdatesMapName)
    become {
      case ss: CurrentGameStatus =>
        hazelcastMap.put(ss.now.server.server, ss.toJson)
        hazelcastTopic.publish(ss.toJson)
    }
  })
  system.eventStream.subscribe(publisherActor, classOf[ServerStatus])
  system.eventStream.subscribe(publisherActor, classOf[CurrentGameStatus])
  system.awaitTermination()
  system.shutdown()

}