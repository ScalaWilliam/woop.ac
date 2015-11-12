package acleague.ranker.achievements.immutable

import acleague.enrichers.JsonGamePlayer

/**
  * Created by William on 11/11/2015.
  */

object FragMaster extends Incremental {

  override def levels = List(500, 1000, 2000, 5000, 10000)

  override type InputType = JsonGamePlayer

  override def filter(inputType: JsonGamePlayer): Option[Int] = {
    Option(inputType.frags)
  }

  override def title(level: Int): String = s"Frag Master: $level"

  override def description(level: Int): String = Map(
    500 -> "Well, that's a start.",
    1000 -> "Already lost count.",
    2000 -> "I'm quite good at this!",
    5000 -> "I've seen blood.",
    10000 -> "That Rambo guy got nothin' on me."

  ).getOrElse(level, s"Achieve $level frags")

  override def completedEventTitle(level: Int): String = s"Frag Master level $level"
}
