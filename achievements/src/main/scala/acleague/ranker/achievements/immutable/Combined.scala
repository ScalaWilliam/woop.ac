package acleague.ranker.achievements.immutable

import acleague.enrichers.{JsonGamePlayer, JsonGameTeam, JsonGame}

case class Combined
(captureMapCompletion: CaptureMapCompletion,
 captureMaster: CaptureMaster,
 cubeAddict: CubeAddict.CoreType,
 dDay: DDay,
 flagMaster: FlagMaster.CoreType,
 fragMaster: FragMaster.CoreType,
 maverick: Maverick,
 playerStatistics: PlayerStatistics,
 slaughterer: Slaughterer,
 tdmLover: TdmLover,
 terribleGame: TerribleGame,
 tosokLover: TosokLover
) {
  def include(jsonGame: JsonGame, jsonGameTeam: JsonGameTeam, jsonGamePlayer: JsonGamePlayer) = {
    var me = this
    var newAchievements = scala.collection.mutable.ListBuffer.empty[Any]
    captureMapCompletion match {
      case a: CaptureMapCompletion.Achieving =>
        val newCmc = a.include(jsonGame, jsonGameTeam, jsonGamePlayer)
        newCmc.map(_.fold(identity, identity)).foreach { m => me = me.copy(captureMapCompletion = m) }
        newCmc.foreach {
          case Right(achieved) => newAchievements += achieved
          case _ =>
        }
      case _ =>
    }
  }
}

