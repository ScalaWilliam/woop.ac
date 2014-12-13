package acleague.actors

import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.publishers.GamePublisher
import GamePublisher.ConnectionOptions
import acleague.publishers.GamePublisher
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}
object GamePusherActor {
  def props(options: ConnectionOptions) = Props(new GamePusherActor(options))
}
class GamePusherActor(options: ConnectionOptions) extends Act with ActorLogging {
  whenStarting {
    log.info("Started message publisher actor with options {}", options)
  }
  become {
    case game @ GameXmlReady(xml) =>
      log.info("Received game to publish: {}", game)
      val publishResult = GamePublisher.publishMessage(options)(game)
      context.system.eventStream.publish(publishResult)
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