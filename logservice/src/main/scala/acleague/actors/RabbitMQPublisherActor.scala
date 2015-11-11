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

  whenStarting {
    log.info("Started up rabbitmq publisher")
  }
  become {
    case GameJsonFound(jsonGame) =>
      channel.exchangeDeclare(exchangeName, "fanout")
      channel.basicPublish(exchangeName, "game-found", null, jsonGame.toJson.toString().getBytes("UTF-8"))
  }
}
