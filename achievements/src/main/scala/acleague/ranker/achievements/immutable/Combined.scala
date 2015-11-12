package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGamePlayer, JsonGameTeam, JsonGame}
import acleague.ranker.achievements.MapAchievements
import play.api.libs.json.JsObject

object Combined {
  def empty = Combined(
    flagMaster = FlagMaster.empty,
    fragMaster = FragMaster.empty,
    captureMaster = CaptureMaster.empty(maps = MapAchievements.captureMaster.map(_.name).toList)
  )
}
case class Combined
(
  flagMaster: FlagMaster.CoreType,
  fragMaster: FragMaster.CoreType,
  captureMaster: CaptureMaster
) {
  def asJson = JsObject(Map(
    "flagMaster" -> flagMaster.info ++ flagMaster.genericInfo,
    "fragMaster" -> fragmaster.info ++ fragMaster.genericInfo,
    "captureMaster" ->

  ))
  def include(jsonGame: JsonGame, jsonGameTeam: JsonGameTeam, jsonGamePlayer: JsonGamePlayer):
  Option[(Combined, List[AchievedProfileAchievement], List[EventAchieved])] = {
    var me = this
    val achievements = scala.collection.mutable.ListBuffer.empty[AchievedProfileAchievement]
    val events = scala.collection.mutable.ListBuffer.empty[EventAchieved]
    PartialFunction.condOpt(flagMaster) {
      case a: FlagMaster.Achieving =>
        a.include((jsonGame, jsonGamePlayer)).foreach {
          case Left((achieving, Some(achieved))) =>
            me = me.copy(flagMaster = achieving)
            achievements += achieved
            events += achieved
          case Left((achieving, None)) =>
            me = me.copy(flagMaster = achieving)
          case Right(achieved) =>
            me = me.copy(flagMaster = achieved)
            achievements += achieved
            events += achieved
        }
    }
    PartialFunction.condOpt(captureMaster) {
      case a: CaptureMaster.Achieving =>
        val included = a.includeGame(jsonGame, jsonGameTeam, jsonGamePlayer)
        included.collect { case (_, Some(completedMap)) => completedMap }.foreach { cm =>
          events += cm
        }
        included.collect {
          case (Left(achieving), _) =>
            me = me.copy(captureMaster = achieving)
          case (Right(achieved), _) =>
            me = me.copy(captureMaster = achieved)
            achievements += achieved
            events += achieved
        }
    }
    if (me == this) Option.empty
    else Option((me, achievements.toList, events.toList))
  }
}

