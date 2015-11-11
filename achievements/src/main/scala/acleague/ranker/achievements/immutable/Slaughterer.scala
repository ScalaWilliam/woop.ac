package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGame, JsonGamePlayer}

/**
  * Created by William on 11/11/2015.
  */
sealed trait Slaughterer

object Slaughterer {

  case class Achieved(frags: Int) extends Slaughterer

  case object NotAchieved extends Slaughterer {
    def processGame(game: JsonGame,
                    player: JsonGamePlayer,
                    isRegisteredPlayer: JsonGamePlayer => Boolean): Option[Achieved] = {
      for {
        "ctf" <- List(game.mode)
        firstTeam <- game.teams
        if firstTeam.players.contains(player)
        secondTeam <- game.teams; if secondTeam != firstTeam
        if player.frags >= 80
        if secondTeam.players.exists(isRegisteredPlayer)
      } yield Achieved(player.frags)

    }.headOption
  }

}
