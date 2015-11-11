package acleague.mserver

import acleague.enrichers.JsonGame

/**
* Created by William on 11/11/2015.
*/
object MultipleServerParser {
  def empty: MultipleServerParser = MultipleServerParserProcessing(servers = Map.empty)
}

sealed trait MultipleServerParser {
  def process(line: String): MultipleServerParser
}

case class MultipleServerParserFoundGame(cg: JsonGame, next: MultipleServerParserProcessing)
  extends MultipleServerParser {
  def process(line: String): MultipleServerParser = {
    next.process(line)
  }
  def goodString: Option[String] = {
    cg.validate.toOption.map { game =>
    s"${game.id}\t${game.toJson}"}
  }
  def detailString: String = {
    val info = cg.validate.fold(_ => "GOOD\t", b => s"BAD\t$b")
    s"${cg.id}\t$info\t${cg.toJson}"
  }
}

case class MultipleServerParserFailedLine(line: String, next: MultipleServerParserProcessing)
  extends MultipleServerParser {
  def process(line: String): MultipleServerParser = next
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

