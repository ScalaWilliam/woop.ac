package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGame, JsonGamePlayer}

/**
  * Created by William on 11/11/2015.
  */
sealed trait FlagMaster

object FlagMaster {

  case object Achieved extends FlagMaster
  case class AchievedLevel(level: Int) extends FlagMaster
  abstract case class Achieving(count: Int, target: Int) extends FlagMaster {
    def increment: Achieving
    def includeGame(jsonGame: JsonGame, jsonGamePlayer: JsonGamePlayer): Either[Achieved.type, (Achieving, Option[AchievedLevel])] = {
      this match {
        case hn: Achieving with HasNext if count + 1 == target =>
          Right(hn.next -> Option(AchievedLevel(target)))
        case hn: Achieving =>
          Right(hn.increment -> Option.empty)
        case Achieving1000(thousandCount) if thousandCount + 1 == this.target =>
          Left(Achieved)
      }
    }
  }
  sealed trait HasNext {
    def next: Achieving
  }
  case class Achieving50(override val count: Int) extends Achieving(count, 50) with HasNext {
    def next = Achieving100(count + 1)
    def increment = Achieving50(count = count + 1)
  }
  case class Achieving100(override val count: Int) extends Achieving(count, 100) with HasNext {
    def next = Achieving200(count + 1)
    def increment = Achieving100(count = count + 1)
  }
  case class Achieving200(override val count: Int) extends Achieving(count, 200) with HasNext {
    def next = Achieving500(count + 1)
    def increment = Achieving200(count = count + 1)
  }
  case class Achieving500(override val count: Int) extends Achieving(count, 500) with HasNext  {
    def next = Achieving1000(count + 1)
    def increment = Achieving500(count = count + 1)
  }
  case class Achieving1000(override val count: Int) extends Achieving(count, 1000) {
    override def increment: Achieving = Achieving1000(count = count + 1)
  }


}
