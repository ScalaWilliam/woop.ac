package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGamePlayer, JsonGameTeam, JsonGame}
import play.api.libs.json._

sealed trait CaptureMaster extends ProfileAchievement {

  override def title = "Capture Master"

  override def description = "Complete the selected CTF maps, both sides 3 times"

  def allMaps: List[CaptureMapCompletion]

  def jsonMaps = JsArray(allMaps.map { a =>
    import a._
    val rvsfText = s"$rvsf/$target"
    val claText = s"$cla/$target"
    JsObject(Map("map" -> JsString(map), "rvsf" -> JsString(rvsfText), "cla" -> JsString(claText)))
  })

  override def info: JsObject = JsObject(Map("table" -> jsonMaps))

}

object CaptureMaster {

  def empty(maps: List[String]): Achieving =
    Achieving(
      achieving = maps.map(map => CaptureMapCompletion.empty(map)),
      achieved = List.empty
    )

  case class Achieving(achieving: List[CaptureMapCompletion.Achieving],
                       achieved: List[CaptureMapCompletion.Achieved])
    extends CaptureMaster
    with ProgressingProfileAchievement {
    def includeGame(jsonGame: JsonGame,
                    jsonGameTeam: JsonGameTeam,
                    jsonGamePlayer: JsonGamePlayer):
    Option[(Either[Achieving, Achieved], Option[CaptureMapCompletion.Achieved])] = {
      val withIncluded = achieving.map(a => a.include(jsonGame, jsonGameTeam, jsonGamePlayer).getOrElse(Left(a)))
      val nextMe = copy(
        achieving = withIncluded.flatMap(_.left.toSeq),
        achieved = withIncluded.flatMap(_.right.toSeq)
      )
      if (nextMe == this) Option.empty
      else Option {
        val newlyCompleted = (nextMe.achieved.toSet -- achieved.toSet).headOption
        val myNextIteration =
          if (nextMe.achieving.isEmpty) Right(Achieved(nextMe.achieved))
          else Left(nextMe)
        myNextIteration -> newlyCompleted
      }
    }

    override def allMaps: List[CaptureMapCompletion] = achieving ++ achieved
  }

  case class Achieved(achieved: List[CaptureMapCompletion.Achieved])
    extends CaptureMaster
    with AchievedProfileAchievement
    with EventAchieved {
    override def allMaps: List[CaptureMapCompletion] = achieved

    override def eventTitle(name: String): String = s"$name became Capture Master"
  }

}
