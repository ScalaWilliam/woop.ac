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

  override def title(level: Int): String = s"Flag Master: $level"

  override def description(level: Int): String = Map(
  50 -> "What's that blue thing?",
  100 -> "I'm supposed to bring this back?",
  200 -> "What do you mean it's TDM?",
  500 -> "Yeah, I know where it goes.",
  1000 -> "Can I keep one at least?"
  ).getOrElse(level, s"Achieve $level flags")

  override def completedEventTitle(level: Int): String = s"Flag Master level $level"
}
