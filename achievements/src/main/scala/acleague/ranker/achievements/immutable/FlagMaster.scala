package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGame, JsonGamePlayer}

/**
  * Created by William on 11/11/2015.
  */

object FlagMaster extends Incremental {

  override def levels = List(50, 100, 200, 500, 1000)

  override type InputType = (JsonGame, JsonGamePlayer)

  override def filter(inputType: (JsonGame, JsonGamePlayer)): Option[Int] = {
    inputType match {
      case (game, player) if game.mode == "ctf" => player.flags
      case _ => None
    }
  }
}
