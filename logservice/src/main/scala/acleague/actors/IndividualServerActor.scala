package acleague.actors

import java.time.ZonedDateTime

import acleague.actors.ReceiveMessages.RealMessage
import acleague.enrichers.{JodaTimeToZDT, EnrichFoundGame}
import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.ingesters._
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}
import org.joda.time.format.ISODateTimeFormat

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

        _ = { if ( !(duration >= 10) ) log.info(s"Rejecting this game because duration is $duration, expecting at least 10")}
        if duration >= 10

        enrichedGame @ GameXmlReady(xmlContent) = EnrichFoundGame(fg)(date, serverId, duration)

        xmlElem = scala.xml.XML.loadString(xmlContent)

        fragsSeq = (xmlElem \\ "player").flatMap(_ \ "@frags").map(_.text.toInt)
        _ = { if ( !(fragsSeq.size >= 4) ) log.info(s"Rejecting this game because there are <4 frag counts: $fragsSeq")}
        if fragsSeq.size >= 4

        minimumTeamSize = (xmlElem \ "team").map(_ \ "player").map(_.size).min
        _ = { if ( !(minimumTeamSize >= 2) ) log.info(s"Rejecting this game because one team has $minimumTeamSize number of players, minimum of 2 required")}

        averageFrags = fragsSeq.sum / fragsSeq.size
        _ = { if ( !(averageFrags >= 15) ) log.info(s"Rejecting this game because average frags < 15: $averageFrags")}
        if averageFrags >= 15

        jsonGame = EnrichFoundGame.jsonGame(fg, JodaTimeToZDT(date), serverId, duration)

      } {
        lastGameO = Option(enrichedGame)
        context.system.eventStream.publish(enrichedGame)
        context.system.eventStream.publish(jsonGame)
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
          id = (lastGameXml \@ "id").toInt
          lastMode = lastGameXml \@ "mode"
          lastMap = lastGameXml \@ "map"
          if lastMode == demoMode
          if lastMap == demoMap
          gameDemo = GameDemoFound(id, serverId, recorded, written)
        } {
          context.system.eventStream.publish(gameDemo)
        }
      }
      
  }
}
case class GameDemoFound(gameId: Int, serverId: String, demoRecorded: DemoRecorded, demoWritten: DemoWritten)
object GameDemoFound {
  def example = GameDemoFound(2141423, "wut", DemoRecorded("a", "b", "c", "d"), DemoWritten("e", "f"))
}
