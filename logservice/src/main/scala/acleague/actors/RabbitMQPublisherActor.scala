package acleague.actors

import acleague.enrichers.GameJsonFound
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}
import com.rabbitmq.client.ConnectionFactory

object RabbitMQPublisherActor {
  def props(hostname: String, exchangeName: String) = {
    Props(new RabbitMQPublisherActor(hostname, exchangeName))
  }
}
class RabbitMQPublisherActor(hostname: String, exchangeName: String) extends Act with ActorLogging {

  val cf = new ConnectionFactory
  cf.setHost(hostname)
  val conn = cf.newConnection()
  val channel = conn.createChannel()
  channel.exchangeDeclare(exchangeName, "fanout", true)

  whenStarting {
    log.info("Started up rabbitmq publisher")
    channel.basicPublish(exchangeName, "test", null, "test".getBytes("UTF-8"))

  }
  become {
    case GameJsonFound(jsonGame) =>
      channel.basicPublish(exchangeName, "game-found", null, jsonGame.toJson.toString().getBytes("UTF-8"))
  }
}
