package acleague.ranker.achievements

import acleague.ranker.achievements.MapAchievements.AcMap

import scala.xml.{Elem, Node}

/**
 * Created by William on 10/01/2015.
 */
object Imperative {
  implicit class attText(elem: Node) {
    def attText(name: String) = (elem \ s"@$name").text
  }
  trait Achievement {
    def isCompleted: Boolean
  }
  class DDayAchievement(var today: String = "", var counter: Int = 0) extends Achievement {
    val target = 80
    def isCompleted: Boolean = counter == target
    def include(day: String): Unit = {
      if ( !isCompleted ) {
        if ( today != day ) {
          counter = 0
          today = day
        }
        counter = counter + 1
      }
    }
    override def toString = s"""DDayAchievement(today = $today, counter = $counter, completed = $isCompleted)"""
  }
  class FlagMaster(var flags: Int = 0) extends Achievement {
    val levels: Set[Int] = Set(50, 100, 200, 500, 1000)
    val starter = levels.min
    val flagMaster = levels.max
    def currentLevel: Option[Int] = {
      val levelsPassed = levels.filter(_ <= flags)
      if ( levelsPassed.isEmpty ) None
      else Option(levelsPassed.max)
    }
    def nextLevel: Option[Int] = {
      currentLevel match {
        case None => Option(starter)
        case Some(`flagMaster`) => None
        case Some(other) => Option(levels.filter(_ > other).min)
      }
    }
    def isCompleted = currentLevel.contains(flagMaster)
    def remainingToNext: Option[Int] = nextLevel.map(_ - flags)
    override def toString = s"""FlagMaster(flags = $flags, isCompleted = $isCompleted, currentLevel = $currentLevel, nextLevel = $nextLevel, remainingToNext = $remainingToNext)"""
  }
  class FragMaster(var frags: Int = 0) extends Achievement {
    val levels: Set[Int] = Set(500, 1000, 2000, 5000, 10000)
    val starter = levels.min
    val flagMaster = levels.max
    def currentLevel: Option[Int] = {
      val levelsPassed = levels.filter(_ <= frags)
      if ( levelsPassed.isEmpty ) None
      else Option(levelsPassed.max)
    }
    def nextLevel: Option[Int] = {
      currentLevel match {
        case None => Option(starter)
        case Some(`flagMaster`) => None
        case Some(other) => Option(levels.filter(_ > other).min)
      }
    }
    def isCompleted = currentLevel.contains(flagMaster)
    def remainingToNext: Option[Int] = nextLevel.map(_ - frags)
    override def toString = s"""FragMaster(frags = $frags, isCompleted = $isCompleted, currentLevel = $currentLevel, nextLevel = $nextLevel, remainingToNext = $remainingToNext)"""
  }
  class CubeAddict(var minutes: Int = 0) extends Achievement {
    def hours = minutes / 60
    val levels: Set[Int] = Set(5, 10, 20, 50, 100)
    val starter = levels.min
    val cubeAddict = levels.max
    def currentLevel: Option[Int] = {
      val levelsPassed = levels.filter(_ <= hours)
      if ( levelsPassed.isEmpty ) None
      else Option(levelsPassed.max)
    }
    def nextLevel: Option[Int] = {
      currentLevel match {
        case None => Option(starter)
        case Some(`cubeAddict`) => None
        case Some(other) => Option(levels.filter(_ > other).min)
      }
    }
    def isCompleted = currentLevel.contains(cubeAddict)
    def hoursRemainingToNext: Option[Int] = nextLevel.map(_ - hours)
    override def toString = s"""CubeAddict(minutes = $minutes, hours = $hours, isCompleted = $isCompleted, currentLevel = $currentLevel, nextLevel = $nextLevel, remainingToNext = $hoursRemainingToNext)"""
  }
  class MapCompletionAchievement(map: AcMap, var rvsfRemain: Int = 3, var claRemain: Int = 3) extends Achievement {
    def rvsfProgress = rvsfTarget - rvsfRemain
    def claProgress = claTarget - claRemain
    def isCompleted = rvsfRemain == 0 && claRemain == 0
    def remaining = rvsfRemain + claRemain
    def completed = target - remaining
    def target = claTarget + rvsfTarget
    def rvsfTarget = 3
    def claTarget = 3
    override def toString = s"MapCompletionAchievement(map = $map, rvsfRemain = $rvsfRemain, claRemain = $claRemain)"
  }
  class TdmLover(var count: Int = 0 ) extends Achievement {
    val target = 25
    def remaining = target - count
    def isCompleted = count == target
    def progress = count
    override def toString =
      s"""TdmLover(completed = $isCompleted, count = $count, remaining = $remaining, target = $target)"""
  }
  class TosokLover(var count: Int = 0 ) extends Achievement {
    val target = 25
    def remaining = target - count
    def isCompleted = count == target
    def progress = count
    override def toString =
      s"""TdmLover(completed = $isCompleted, count = $count, remaining = $remaining, target = $target)"""
  }
  class CaptureMaster extends Achievement {
    val maps = MapAchievements.captureMaster.map(m => m -> new MapCompletionAchievement(m)).toMap
    val target = maps.size
    def completed = maps.count(_._2.isCompleted)
    def remaining = target - completed
    def isCompleted = remaining == 0
    def progress = completed
    override def toString =
      s"""CaptureMaster(completed = $isCompleted, remaining = $remaining, target = $target, maps = $maps)"""
  }
  class Socialite[UserId](self: UserId) extends Achievement {
    val target = 20
    val enemiesSet = scala.collection.mutable.Set.empty[UserId]
    def include(userId: UserId): Unit = {
      if ( userId != self ) {
        enemiesSet += userId
      }
    }
    def completed = enemiesSet.size
    override def isCompleted = completed == target
    def remaining = target - completed
  }

  class User[UserId]
  (
    val id: UserId,
    var flags: Int = 0,
    var frags: Int = 0,
    var soloFlagger: Option[String] = None,
    var slaughterer: Option[String] = None,
    var hasTerribleGame: Option[String] = None,
    var timePlayed: Int = 0,
    var gamesPlayed: Int = 0,
    var isTdmLover: Option[String] = None,
    var isCubeAddict: Option[String] = None,
    var isTosokLover: Option[String] = None,
    var isFlagMaster: Option[String] = None,
    var hasCubeAddicts: collection.mutable.Buffer[(Int, String)] = collection.mutable.Buffer.empty,
    var hasFragMasters: collection.mutable.Buffer[(Int, String)] = collection.mutable.Buffer.empty,
    var hasFlagMasters: collection.mutable.Buffer[(Int, String)] = collection.mutable.Buffer.empty,
    var isFragMaster: Option[String] = None,
    var hasDDay: Option[String] = None,
    val playedGames: collection.mutable.Set[String] = collection.mutable.Set.empty,
    var isCaptureMaster: Option[String] = None,
    var socialiteEarned: Int = 0,
    var flagMaster: FlagMaster = new FlagMaster(),
    var dDayAchievement: DDayAchievement = new DDayAchievement(),
    var tosokLover: TosokLover = new TosokLover(),
    var tdmLover: TdmLover = new TdmLover(),
    var fragMaster: FragMaster = new FragMaster(),
    var cubeAddict: CubeAddict = new CubeAddict(),
    val captureMaster: CaptureMaster = new CaptureMaster
  ) {
    val socialite = new Socialite(id)
    override def toString =
      s"""User(id = $id, soloFlagger = $soloFlagger, hasCubeAddicts = $hasCubeAddicts, hasFragMasters = $hasFragMasters, hasFlagMasters = $hasFlagMasters, isTosokLover = $isTosokLover, isFlagMaster = $isFlagMaster, isFragMaster = $isFragMaster, isCubeAddict = $isCubeAddict, isTdmLover = $isTdmLover, slaughterer = $slaughterer, terribleGames = $hasTerribleGame, gamesPlayed = $gamesPlayed, flags = $flags, frags = $frags, timePlayed = $timePlayed, isCaptureMaster = $isCaptureMaster, cubeAddict = $cubeAddict, dDayAchievement = $dDayAchievement, playedGames = $playedGames, tosokLover = $tosokLover, tdmLover = $tdmLover, flagMaster = $flagMaster, fragMaster = $fragMaster, captureMaster = $captureMaster"""

    def toXml =
      <user-record
        id={s"$id"}
      >
        <counts flags={s"$flags"} frags={s"$frags"} games={s"$gamesPlayed"} time={s"PT${timePlayed}M"}/> {
        for {gameId <- playedGames.toList}
        yield <played-in-game game-id={gameId}/>
        }
        <achievements>
          <solo-flagger achieved={soloFlagger.isDefined.toString} at-game={soloFlagger.orNull}/>
          <slaughterer achieved={slaughterer.isDefined.toString} at-game={slaughterer.orNull}/>
          <terrible-game achieved={hasTerribleGame.isDefined.toString} at-game={hasTerribleGame.orNull}/>
          <dday achieved={hasDDay.isDefined.toString} at-game={hasDDay.orNull}/>
          <tdm-lover achieved={isTdmLover.isDefined.toString} achieved-at-game={isTdmLover.orNull} progress={tdmLover.progress.toString} target={tdmLover.target.toString} remain={tdmLover.remaining.toString}/>
          <tosok-lover achieved={isTosokLover.isDefined.toString} achieved-at-game={isTosokLover.orNull} progress={tosokLover.progress.toString} target={tosokLover.target.toString} remain={tosokLover.remaining.toString}/>
          {for { (level, atGame) <- hasFlagMasters } yield <flag-master achieved="true" at-game={atGame} level={level.toString}/>}
          { if ( !flagMaster.isCompleted ) {
            <flag-master achieved="false" level={flagMaster.currentLevel.map(_.toString).orNull} next-level={flagMaster.nextLevel.map(_.toString).orNull}
            total-in-level={flagMaster.nextLevel.flatMap(x => flagMaster.currentLevel.map(l => x - l)).map(_.toString).orNull}
            progress-in-level={flagMaster.currentLevel.map(n => flagMaster.flags -n).map(_.toString).orNull}
            remaining-in-level={flagMaster.remainingToNext.map(_.toString).orNull}/>
          }}
          { for { (level, atGame) <- hasFragMasters } yield <frag-master achieved="true" at-game={atGame} level={level.toString}/> }
          { if ( !fragMaster.isCompleted ) {
            <frag-master achieved="false" level={fragMaster.currentLevel.map(_.toString).orNull} next-level={fragMaster.nextLevel.map(_.toString).orNull}
            total-in-level={fragMaster.nextLevel.flatMap(x => fragMaster.currentLevel.map(l => x - l)).map(_.toString).orNull}
            progress-in-level={fragMaster.currentLevel.map(n => fragMaster.frags -n).map(_.toString).orNull}
            remaining-in-level={fragMaster.remainingToNext.map(_.toString).orNull}/>
          }
          }
          { for { (level, atGame) <- hasCubeAddicts } yield <cube-addict achieved="true" at-game={atGame} level={level.toString}/> }
          { if ( !cubeAddict.isCompleted ) {
            <cube-addict achieved="false" level={cubeAddict.currentLevel.map(_.toString).orNull} next-level={cubeAddict.nextLevel.map(_.toString).orNull}
            total-in-level={cubeAddict.nextLevel.flatMap(x => cubeAddict.currentLevel.map(l => x - l)).map(_.toString).orNull}
            progress-in-level={cubeAddict.currentLevel.map(n => cubeAddict.hours -n).map(_.toString).orNull}
            remaining-in-level={cubeAddict.hoursRemainingToNext.map(_.toString).orNull}/>
          }
          }
          <capture-master achieved={isCaptureMaster.isDefined.toString}
                      progress={captureMaster.progress.toString}
                      remaining={captureMaster.remaining.toString}
                      target={captureMaster.target.toString}
          at-game={isCaptureMaster.orNull}
          >
            {
            for { (map, mapCompletion) <- captureMaster.maps.toList }
              yield <map-completion
              mode={map.mode}
              map={map.name}
              is-completed={mapCompletion.isCompleted.toString}
              progress-cla={mapCompletion.claProgress.toString}
              progress-rvsf={mapCompletion.rvsfProgress.toString}
              target-cla={mapCompletion.claTarget.toString}
              target-rvsf={mapCompletion.rvsfTarget.toString}
              remaining-cla={mapCompletion.claRemain.toString}
              remaining-rvsf={mapCompletion.rvsfRemain.toString}
              />
            }
          </capture-master>
        </achievements>
      </user-record>

  }
  case class Player(name: String, host: String, flags: Option[Int], frags: Int)
  case class Team(name: String, players: Set[Player])
  case class Game(id: String, date: String, duration: Int, acMap: AcMap, teams: Set[Team]) {
    private def game = this
    def playersAsUsers[UserId](userRepository: UserRepository[UserId]) = for {
      team <- game.teams
      player <- team.players
      user <- userRepository(player)
    } yield (user.id, user, player)
  }

  def createGameFromXml(gameXml: Elem) = {
    Game(
      id = gameXml.attText("id"),
      date = gameXml.attText("date"),
      duration = gameXml.attText("duration").toInt,
      acMap = AcMap(mode = gameXml.attText("mode"), name = gameXml.attText("map")),
      teams = (for {
        team <- gameXml \ "team"
        teamName = team attText "name"
      } yield Team(name = teamName,
        players = (
          for {
            p <- team \ "player"
            host = p attText "host"
            name = p attText "name"
            frags = (p attText "frags").toInt
            flags = p.attribute("flags").toSeq.flatMap(x => x).map(_.text.toInt).headOption
          } yield Player(name = name, host = host, flags = flags, frags = frags)
          ).toSet)).toSet
    )
  }

  trait UserEvent {
    def asXml: Elem
  }
  case object BecameCaptureMaster extends UserEvent {
    override def asXml = <became-capture-master-event/>
  }
  case object Slaughter extends UserEvent {
    override def asXml = <slaughtered-event/>
  }
  case object SoloFlagger extends UserEvent {
    override def asXml = <solo-flagger-event/>
  }
  case object TerribleGame extends UserEvent {
    override def asXml = <terrible-game/>
  }
  case object AchievedDDay extends UserEvent {
    override def asXml = <achieved-dday-event/>
  }
  case class CompletedMap(acMap: AcMap) extends UserEvent {
    override def asXml = <completed-map-event mode={acMap.mode} map={acMap.name}/>
  }
  case class AchievedNewFlagMasterLevel(newLevelFlags: Int) extends UserEvent {
    override def asXml = <achieved-new-flag-master-level new-level-flags={newLevelFlags.toString}/>
  }
  case object BecameFlagMaster extends UserEvent {
    override def asXml = <became-flag-master/>
  }
  case class AchievedNewFragMasterLevel(newLevelFrags: Int) extends UserEvent {
    override def asXml = <achieved-new-frag-master-level new-level-frags={newLevelFrags.toString}/>
  }
  case object BecameFragMaster extends UserEvent {
    override def asXml = <became-frag-master/>
  }
  case class AchievedNewCubeAddictLevel(newLevelHours: Int) extends UserEvent {
    override def asXml = <achieved-new-cube-addict-level new-level-hours={newLevelHours.toString}/>
  }
  case object BecameCubeAddict extends UserEvent {
    override def asXml = <became-cube-addict/>
  }
  case object BecameTdmLover extends UserEvent {
    override def asXml = <became-tdm-lover/>
  }
  case object BecameTosokLover extends UserEvent {
    override def asXml = <became-tosok-lover/>
  }
  type EmmittedEvents[UserId] = collection.immutable.Set[(UserId, UserEvent)]
  type UserRepository[UserId] = Player => Option[User[UserId]]
  type AffectedUsers[UserId] = Set[UserId]
  case class AcceptanceResult[UserId](affectedUsers: Set[UserId], emmittedEvents: Set[(UserId, UserEvent)])
  def acceptGame[UserId](userRepository: UserRepository[UserId])(game: Game): AcceptanceResult[UserId] = {
    val events = scala.collection.mutable.Buffer.empty[(UserId, UserEvent)]
    val affectedUsers = (
      for {
        (userId, _, _) <- game.playersAsUsers(userRepository)
      } yield userId
      ).toSet
    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
    } {
      user.playedGames += game.id
      for {addFlags <- player.flags} {
        user.flags = user.flags + addFlags
      }
      user.frags = user.frags + player.frags
      user.timePlayed = user.timePlayed + game.duration
      user.gamesPlayed = user.gamesPlayed + 1
    }

    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
      if player.frags >= 80
      if !user.slaughterer.isDefined
    } {
      user.slaughterer = Option(game.id)
      events += userId -> Slaughter
    }

    if ( game.acMap.mode == "ctf" ) {
      for {
        team <- game.teams
        teamFlags = team.players.flatMap(_.flags).sum
        if teamFlags >= 5
        enemyFlags = (game.teams - team).flatMap(_.players).flatMap(_.flags).sum
        if teamFlags > enemyFlags
        player <- team.players
        playerFlags <- player.flags
        if teamFlags == playerFlags
        user <- userRepository(player)
        if !user.soloFlagger.isDefined
      } {
        user.soloFlagger = Option(game.id)
        events += user.id -> SoloFlagger
      }
    }

    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
      if !user.hasTerribleGame.isDefined
      if player.frags <= 15
    } {
      user.hasTerribleGame = Option(game.id)
      events += userId -> TerribleGame
    }

    // dday in a day achievement
    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
      if !user.hasDDay.isDefined
    } {
      user.dDayAchievement.include(game.date.substring(0, 10))
      if ( user.dDayAchievement.isCompleted ) {
        user.hasDDay = Option(game.id)
        events += user.id -> AchievedDDay
      }
    }

    if ( game.acMap.mode == "team one shot, one kill") {
      for {
        (userId, user, player) <- game.playersAsUsers(userRepository)
        if !user.isTosokLover.isDefined
      } {
        user.tosokLover.count = user.tosokLover.count + 1
        if ( user.tosokLover.isCompleted ) {
          user.isTosokLover = Option(game.id)
          events += user.id -> BecameTosokLover
        }
      }
    }
    if ( game.acMap.mode == "team deathmatch") {
      for {
        (userId, user, player) <- game.playersAsUsers(userRepository)
        if !user.isTdmLover.isDefined
      } {
        user.tdmLover.count = user.tdmLover.count + 1
        if ( user.tdmLover.isCompleted ) {
          user.isTdmLover = Option(game.id)
          events += user.id -> BecameTdmLover
        }
      }
    }
    // flag master achievement
    if ( game.acMap.mode == "ctf") {
      for {
        (userId, user, player) <- game.playersAsUsers(userRepository)
        flags <- player.flags
        flagMaster = user.flagMaster
        if !user.isFlagMaster.isDefined
      } {
        val initialLevelO = flagMaster.currentLevel
        val initiallyCompleted = flagMaster.isCompleted
        flagMaster.flags = flagMaster.flags + flags
        val newLevelO = flagMaster.currentLevel
        if ( newLevelO != initialLevelO ) {
          for { newLevel <- newLevelO } {
            events += userId -> AchievedNewFlagMasterLevel(newLevel)
            user.hasFlagMasters += newLevel -> game.id
            if (!initiallyCompleted && flagMaster.isCompleted) {
              user.isFlagMaster = Option(game.id)
              events += userId -> BecameFlagMaster
            }
          }
        }
      }
    }

    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
      frags = player.frags
      fragMaster = user.fragMaster
      if !user.isFragMaster.isDefined
    } {
      val initialLevelO = fragMaster.currentLevel
      val initiallyCompleted = fragMaster.isCompleted
      fragMaster.frags = fragMaster.frags + frags
      val newLevelO = fragMaster.currentLevel
      if ( newLevelO != initialLevelO ) {
        for { newLevel <- newLevelO } {
          events += userId -> AchievedNewFragMasterLevel(newLevel)
          user.hasFragMasters += newLevel -> game.id
          if (!initiallyCompleted && fragMaster.isCompleted) {
            user.isFragMaster = Option(game.id)
            events += userId -> BecameFragMaster
          }
        }
      }
    }

    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
      frags = player.frags
      if !user.isCubeAddict.isDefined
      cubeAddict = user.cubeAddict
    } {
      val initialLevelO = cubeAddict.currentLevel
      val initiallyCompleted = cubeAddict.isCompleted
      cubeAddict.minutes = cubeAddict.minutes + game.duration
      val newLevelO = cubeAddict.currentLevel
      if ( newLevelO != initialLevelO ) {
        for { newLevel <- newLevelO } {
          user.hasCubeAddicts += newLevel -> game.id
          events += userId -> AchievedNewCubeAddictLevel(newLevel)
          if (!initiallyCompleted && cubeAddict.isCompleted) {
            user.isCubeAddict = Option(game.id)
            events += userId -> BecameCubeAddict
          }
        }
      }
    }

    // map completion achievement
    for {
      team <- game.teams
      player <- team.players
      user <- userRepository(player)
      acMap = game.acMap
      mapCompletion <- user.captureMaster.maps get acMap
      if !mapCompletion.isCompleted
    } {
      if (team.name == "RVSF" && mapCompletion.rvsfRemain > 0) {
        mapCompletion.rvsfRemain = mapCompletion.rvsfRemain - 1
      } else if (team.name == "CLA" && mapCompletion.claRemain > 0) {
        mapCompletion.claRemain = mapCompletion.claRemain - 1
      }
      if (mapCompletion.isCompleted) {
        events += user.id -> CompletedMap(acMap)
      }
      if (mapCompletion.isCompleted && user.captureMaster.isCompleted) {
        user.isCaptureMaster = Option(game.id)
        events += user.id -> BecameCaptureMaster
        // we can trigger side effects here, or at least return new achievements and stuff.
      }
    }
    AcceptanceResult(affectedUsers, (for {
      (userId, event) <- events
    } yield (userId, event)).toSet)
  }

}
