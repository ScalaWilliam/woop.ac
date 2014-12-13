package acleague.actors

import acleague.publishers.DemoPublisher
import acleague.publishers.GamePublisher.ConnectionOptions

import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}

object DemoPusherActor {
  def props(options: ConnectionOptions) = Props(new DemoPusherActor(options))
}
class DemoPusherActor(options: ConnectionOptions) extends Act with ActorLogging {
  whenStarting {
    log.info("Started demo pusher with options {}", options)
  }
  become {
    case demo: GameDemoFound =>
      log.debug("Received demo to publish: {}", demo)
      DemoPublisher(options)(demo)
  }
  whenFailing {
    case (failure, Some(demo: GameDemoFound)) =>
      log.error(failure, "Failed to push demo, trying again: {}", demo)
      import concurrent.duration._
      import context.dispatcher
      context.system.scheduler.scheduleOnce(5.seconds, self, demo)
    case _ =>
  }
}
