package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGameTeam, JsonGamePlayer, JsonGame}

/**
  * Created by William on 12/11/2015.
  */
sealed trait CaptureMapCompletion {
  def map: String
  def target = CaptureMapCompletion.targetPerSide
  def rvsf: Int
  def cla: Int
}
object CaptureMapCompletion {
  val targetPerSide = 3
  case class Achieved(map: String) extends CaptureMapCompletion with EventAchieved {
    override def rvsf: Int = targetPerSide
    override def cla: Int = targetPerSide
    override def eventTitle(name: String): String = s"$name completed $map"
  }
  case class Achieving(map: String, cla: Int, rvsf: Int) extends CaptureMapCompletion {
    def include(jsonGame: JsonGame, jsonGameTeam: JsonGameTeam, jsonGamePlayer: JsonGamePlayer): Option[Either[Achieving, Achieved]] = {
      if ( jsonGame.mode == "ctf" && jsonGame.map.equalsIgnoreCase(map) ) {
        val incrementedTeams =
          if ( jsonGameTeam.name.equalsIgnoreCase("cla") ) (Math.min(cla + 1, targetPerSide), rvsf)
          else (cla, Math.min(rvsf + 1, targetPerSide))
        incrementedTeams match {
          case (`targetPerSide`, `targetPerSide`) =>
            Option(Right(Achieved(map)))
          case (`cla`, `rvsf`) =>
            Option.empty
          case (newCla, newRvsf) =>
            Option(Left(copy(cla = newCla, rvsf = newRvsf)))
        }
      } else Option.empty
    }
  }
  def empty(map: String) = Achieving(map = map, cla = 0, rvsf = 0)
}
