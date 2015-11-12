package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGameTeam, JsonGamePlayer, JsonGame}

/**
  * Created by William on 12/11/2015.
  */
sealed trait CaptureMapCompletion {
  def modeMap: CaptureMapCompletion.ModeMap
}
object CaptureMapCompletion {
  case class ModeMap(mode: String, map: String) {
    def gameMatches(jsonGame: JsonGame) = jsonGame.mode.equalsIgnoreCase(mode) && jsonGame.map.equalsIgnoreCase(map)
  }
  val targetPerSide = 3
  case class Achieved(modeMap: ModeMap) extends CaptureMapCompletion
  case class Achieving(modeMap: ModeMap, cla: Int, rvsf: Int) extends CaptureMapCompletion {
    def include(jsonGame: JsonGame, jsonGameTeam: JsonGameTeam, jsonGamePlayer: JsonGamePlayer): Option[Either[Achieving, Achieved]] = {
      if ( modeMap.gameMatches(jsonGame) ) {
        val incrementedTeams =
          if ( jsonGameTeam.name.equalsIgnoreCase("cla") ) (Math.max(cla + 1, targetPerSide), rvsf)
          else (cla, Math.max(rvsf + 1, targetPerSide))
        incrementedTeams match {
          case (`cla`, `rvsf`) => Option.empty
          case (`targetPerSide`, `targetPerSide`) => Option(Right(Achieved(modeMap)))
          case (newCla, newRvsf) => Option(Left(copy(cla = newCla, rvsf = newRvsf)))
        }
      } else Option.empty
    }
  }
  def empty(modeMap: ModeMap) = Achieving(modeMap = modeMap, cla = 0, rvsf = 0)
}
