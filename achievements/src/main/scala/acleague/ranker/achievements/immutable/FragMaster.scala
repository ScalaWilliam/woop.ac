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
}
