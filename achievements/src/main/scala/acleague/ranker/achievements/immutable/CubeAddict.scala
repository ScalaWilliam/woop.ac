package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGame, JsonGamePlayer}

/**
  * Created by William on 12/11/2015.
  */

object CubeAddict extends Incremental {

  def hourLevels = List(5, 10, 20, 50, 100, 200)

  override def levels = hourLevels.map(_ * 60)

  override type InputType = JsonGame

  override def filter(inputType: JsonGame): Option[Int] = {
    Option(inputType.duration)
  }

}

