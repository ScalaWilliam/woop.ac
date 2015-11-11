package acleague.mserver

import acleague.ingesters.{GameDuration, ParserState, FoundGame}

/**
  * Created by William on 11/11/2015.
  */

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

object ServerState {
  def empty: ServerState = ServerStateProcessing(
    parserState = ParserState.empty,
    gameDuration = GameDuration.empty
  )
}
