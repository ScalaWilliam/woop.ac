package acleague.actors

import acleague.actors.ReceiveMessages.RealMessage
import acleague.enrichers.EnrichFoundGame
import acleague.ingesters.{FoundGame, ParserState, NothingFound}
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}

object IndividualServerActor {
  def props(serverId: String) = Props(new IndividualServerActor(serverId))
}

class IndividualServerActor(serverId: String) extends Act with ActorLogging {
  var state = NothingFound: ParserState
  become {
    case RealMessage(date, serverName, message) =>
      val previousState = state
      state = state.next(message)
      log.debug("[{}] {}x{} -> {}", serverName, previousState, message, state)
      for {
        fg@FoundGame(header, game) <- Option(state)
      } { context.system.eventStream.publish(EnrichFoundGame(fg)(date, serverId)) }
  }
}