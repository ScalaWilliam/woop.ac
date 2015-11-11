package acleague.ranker.achievements.immutable

import acleague.enrichers.JsonGame

/**
  * Created by William on 11/11/2015.
  */
sealed trait TdmLover
object TdmLover {
  val target = 25
  case object Achieved extends TdmLover
  def empty = Achieving(counter = 0)
  case class Achieving(counter: Int) extends TdmLover {
    def processGame(jsonGame: JsonGame): Option[Either[Achieving, Achieved.type]] = {
      if ( jsonGame.mode == "team deathmatch" ) {
        Option {
          copy(counter = counter + 1) match {
            case Achieving(`target`) => Right(Achieved)
            case other => Left(other)
          }
        }
      } else None
    }
  }

}
