package acleague.actors

import acleague.actors.ReceiveMessages.RealMessage
import acleague.enrichers.EnrichFoundGame
import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.ingesters._
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}

object IndividualServerActor {
  def props(serverId: String) = Props(new IndividualServerActor(serverId))
}

class IndividualServerActor(serverId: String) extends Act with ActorLogging {
  var state = NothingFound: ParserState
  var demoState = NoDemosCollected: DemoCollector
  var lastGameO = Option.empty[GameXmlReady]
  // we'd rather make it easier on ourselves to add games... let's give them default 15, after a restart
  var durationState = GameInProgress(15, 15) : GameDuration
  become {
    case RealMessage(date, serverName, message) =>
      val previousDuration = durationState
      durationState = durationState.next(message)
      log.debug("[{}] ({} --> {}) via: {}", serverName, previousDuration, durationState, message)
      val previousState = state
      state = state.next(message)
      log.debug("[{}] ({} --> {}) via: {}", serverName, previousState, state, message)
      for {
        fg @ FoundGame(header, game) <- Option(state)
        GameFinished(duration) <- Option(durationState)
        if duration >= 10
        enrichedGame = EnrichFoundGame(fg)(date, serverId, duration)
      } {
        lastGameO = Option(enrichedGame)
        context.system.eventStream.publish(enrichedGame)
      }

      val previousDemoState = demoState
      demoState = demoState.next(message)
      log.debug("[{}] ({})x({}) -> ({})", serverName, previousDemoState, message, demoState)
      for {
        demoWritten @ DemoWrittenCollected(recorded @ DemoRecorded(_, demoMode, demoMap, _), written) <- Option(demoState)
      } {
        context.system.eventStream.publish(demoWritten)
        for {
          lastGame <- lastGameO
          lastGameXml = scala.xml.XML.loadString(lastGame.xml)
          id = lastGameXml \@ "id"
          lastMode = lastGameXml \@ "mode"
          lastMap = lastGameXml \@ "map"
          if lastMode == demoMode
          if lastMap == demoMap
          gameDemo = GameDemoFound(id, recorded, written)
        } {
          context.system.eventStream.publish(gameDemo)
        }
      }
      
  }
}
case class GameDemoFound(gameId: String, demoRecorded: DemoRecorded, demoWritten: DemoWritten)
object GameDemoFound {
  def example = GameDemoFound("abcd", DemoRecorded("a", "b", "c", "d"), DemoWritten("e", "f"))
}
