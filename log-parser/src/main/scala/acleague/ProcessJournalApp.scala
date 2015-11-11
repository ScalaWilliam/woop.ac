package acleague

import java.io.FileInputStream
import java.time._
import java.time.format.DateTimeFormatter
import java.util.Locale

import acleague.enrichers.JsonGame
import acleague.ingesters.{DemoCollector, GameDuration, FoundGame, ParserState}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}

import scala.io.Codec

/**
  * Created by William on 11/11/2015.
  */
object ProcessJournalApp extends App {
  val inputSource = args.headOption match {
    case Some("-") => System.in
    case Some(file) => new FileInputStream(file)
    case _ => throw new IllegalArgumentException("Must specify '-' or a file.")
  }

  object ExtractMessage {
    /**
      * @return server identifier & message
      */
    val matcher =
      """Date: (.*), Server: (.*), Payload: (.*)""".r
    val dateFmt = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss ZZZ yyyy")
    def unapply(line: String): Option[(ZonedDateTime, String, String)] = {
      PartialFunction.condOpt(line) {

        case matcher(date, serverId, message) => (
          {
            val jdt = dateFmt.parseDateTime(date).withZone(DateTimeZone.UTC).getMillis
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(jdt), ZoneId.of("UTC"))
          }
          , serverId, message)
      }
    }
  }

  sealed trait MultipleServerParser {
    def process(line: String): MultipleServerParser
  }

  case class MultipleServerParserFoundGame(cg: JsonGame, next: MultipleServerParserProcessing)
    extends MultipleServerParser {
    def process(line: String): MultipleServerParser = {
      next.process(line)
    }
  }

  case class MultipleServerParserFailedLine(line: String, next: MultipleServerParserProcessing)
    extends MultipleServerParser {
    def process(line: String): MultipleServerParser = next
  }
  sealed trait ServerState {
    def next(line: String): ServerState
  }
  case class ServerFoundGame(foundGame: FoundGame, duration: Int) extends ServerState {
    def next(line: String): ServerState = ServerState.empty
  }
  case class ServerStateProcessing(parserState: ParserState, gameDuration: GameDuration) extends ServerState {
    def next(line: String): ServerState = {
      parserState.next(line) match {
        case fg: FoundGame =>
          ServerFoundGame(foundGame = fg, duration = gameDuration.getOrElse(15))
        case _ =>
          copy(
            parserState = parserState.next(line),
            gameDuration = gameDuration.next(line)
          )
      }
    }
  }

  case class CompletedGame(serverState: ServerState)

  object ServerState {
    def empty: ServerState = ServerStateProcessing(
      parserState = ParserState.empty,
      gameDuration = GameDuration.empty
    )
  }

  case class MultipleServerParserProcessing(servers: Map[String, ServerState]) extends MultipleServerParser {
    def process(line: String): MultipleServerParser = {
      line match {
        case ExtractMessage(date, server, message) =>
          servers.getOrElse(server, ServerState.empty).next(message) match {
            case sfg: ServerFoundGame =>
              val jg = JsonGame.build(
                foundGame = sfg.foundGame, date = date.minusMinutes(sfg.duration),
                serverId = server, duration = sfg.duration
              )
              MultipleServerParserFoundGame(jg, copy(servers = servers.updated(server, ServerState.empty)))
            case other =>
              copy(servers = servers.updated(server, other))
          }
        case m =>
          MultipleServerParserFailedLine(line = line, next = this)
      }
    }
  }

  object MultipleServerParser {
    def empty: MultipleServerParser = MultipleServerParserProcessing(servers = Map.empty)
  }

  scala.io.Source.fromInputStream(inputSource)(Codec.UTF8).getLines().scanLeft(MultipleServerParser.empty)(_.process(_))
    .collect { case MultipleServerParserFoundGame(fg, _) => fg }.foreach(println)

}
