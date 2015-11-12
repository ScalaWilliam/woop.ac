package acleague.ranker.achievements.immutable

import acleague.ranker.achievements.immutable.ProfileAchievement
import play.api.libs.json.JsValue

import scala.runtime.ScalaRunTime

/**
  * Created by William on 12/11/2015.
  */
import play.api.libs.json._
trait Incremental { inc =>
  def title(level: Int): String
  def description(level: Int): String
  def completedEventTitle(level: Int): String
  def empty = Achieving(counter = 0, level = levels.head)
  sealed trait CoreType extends ProfileAchievement { me: Product =>
    override def toString = s"${inc.getClass.getSimpleName.dropRight(1)}.${ScalaRunTime._toString(me)}"
    def level: Int
    override def title: String = inc.title(level)
    override def description: String = inc.description(level)
    override def info: JsObject = {
      this match {
        case Achieving(counter, level) =>
          val percentComplete = 0
          JsObject(Map("level" -> JsNumber(level), "percentComplete" -> JsNumber(percentComplete)))
        case other =>
          JsObject(Map("level" -> JsNumber(level)))
      }
    }
  }
  type InputType
  def levels: List[Int]
  sealed trait Achieved extends CoreType with EventAchieved with AchievedProfileAchievement with Product
  case class Completed(level: Int) extends Achieved {
    override def eventTitle(name: String): String = s"$name achieved $title"
  }
  case class AchievedLevel(level: Int) extends Achieved { me: Product =>
    override def eventTitle(name: String): String = s"$name achieved ${inc.completedEventTitle(level)}"
  }
  def filter(inputType: InputType): Option[Int]
  def begin = Achieving(counter = 0, level = levels.head)
  case class Achieving(counter: Int, level: Int) extends CoreType with ProgressingProfileAchievement {
    def include(inputType: InputType): Option[Either[(Achieving, Option[Achieved]), Completed ]] = {
      for {
        increment <- filter(inputType)
        incremented = counter + increment
      } yield {
        if ( incremented >= level ) {
          val nextLevelO = levels.dropWhile(_ <= level).headOption
          nextLevelO match {
            case None => Right(Completed(level))
            case Some(nextLevel) =>
              Left(Achieving(counter = incremented, level = nextLevel) -> Option(AchievedLevel(level = level)))
          }
        } else Left(
          copy(counter = incremented) -> Option.empty
        )

      }
    }
  }
}
