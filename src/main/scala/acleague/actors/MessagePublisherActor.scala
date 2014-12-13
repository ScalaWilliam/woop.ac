package acleague.actors

import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.publishers.MessagePublisher
import MessagePublisher.ConnectionOptions
import acleague.publishers.MessagePublisher
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}
object MessagePublisherActor {
  def props(options: ConnectionOptions) = Props(new MessagePublisherActor(options))
}
class MessagePublisherActor(options: ConnectionOptions) extends Act with ActorLogging {
  whenStarting {
    log.info("Started message publisher actor with options {}", options)
  }
  become {
    case game @ GameXmlReady(xml) =>
      log.info("Received game to publish: {}", game)
      MessagePublisher.publishMessage(options)(game)
  }
  whenFailing {
    case (failure, Some(game: GameXmlReady)) =>
      log.error(failure, "Failed to push game, trying again: {}", game)
      import concurrent.duration._
      import context.dispatcher
      context.system.scheduler.scheduleOnce(5.seconds, self, game)
    case _ =>
  }
}