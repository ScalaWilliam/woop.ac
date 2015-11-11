package acleague.ranker.achievements.immutable

import acleague.enrichers.JsonGamePlayer

/**
  * Created by William on 11/11/2015.
  */
sealed trait TerribleGame
object TerribleGame {
  case class Achieved(frags: Int) extends TerribleGame
  case object NotAchieved extends TerribleGame {
    def processGame(jsonGamePlayer: JsonGamePlayer): Option[Achieved] = {
      if ( jsonGamePlayer.frags <= 15 ) Option(Achieved(jsonGamePlayer.frags))
      else None
    }
  }

}
