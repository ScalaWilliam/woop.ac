package acleague

import acleague.achievements.MapsToComplete
import acleague.achievements.MapsToComplete.AcMap
import scala.xml.{Node, Elem}

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
  class TenGamesInADayAchievement(var today: String = "", var counter: Int = 0) extends Achievement {
    val target = 10
    def isCompleted: Boolean = counter == target
    def include(day: String): Unit = {
      if ( today != day ) {
        counter = 0
        today = day
      }
      counter = counter + 1
    }
    override def toString = s"""TenGamesInADayAchievement(today = $today, counter = $counter)"""
  }
  class FiftyFlagsAchievement(var flags: Int, var overflow: Int = 0) extends Achievement {
    val target = 50
    def progress = flags
    def include(newFlags: Int): Unit = {
      flags = Math.min(target, flags + newFlags)
      overflow = Math.max(0, flags + newFlags - target)
    }
    def remaining = target - flags
    def isCompleted = flags == target
    override def toString = s"FiftyFlagsAchievement(flags = $flags, overflow = $overflow)"
  }
  class ThousandFragsAchievement(var frags: Int, var overflow: Int = 0) extends Achievement {
    val target = 1000
    def progress = frags
    def include(newFrags: Int): Unit = {
      frags = Math.min(target, frags + newFrags)
      overflow = Math.max(0, frags + newFrags - target)
    }
    def remaining = target - frags
    def isCompleted = frags == target
    override def toString = s"ThousandFragsAchievement(flags = $frags, overflow = $overflow)"
  }
  class FiftyGamesAchievement(var games: Int) extends Achievement {
    def progress = games
    def isCompleted = games == target
    def remaining = target - games
    def target = 50
    override def toString = s"FiftyGamesAchievement(games = $games)"
  }
  class TwentyHoursPlayedAchievement(var duration: Int) extends Achievement {
    def progress = Math.floor(duration / 60).toInt
    def isCompleted = progress == target
    def remaining = target - progress
    def target = 20
    override def toString = s"TwentyHoursPlayedAchievement(duration[minutes] = $duration)"
  }
  class MapCompletionAchievement(map: AcMap, var rvsfRemain: Int = 2, var claRemain: Int = 2) extends Achievement {
    def rvsfProgress = rvsfTarget - rvsfRemain
    def claProgress = claTarget - claRemain
    def isCompleted = rvsfRemain == 0 && claRemain == 0
    def remaining = rvsfRemain + claRemain
    def completed = target - remaining
    def target = claTarget + rvsfTarget
    def rvsfTarget = 2
    def claTarget = 2
    override def toString = s"MapCompletionAchievement(map = $map, rvsfRemain = $rvsfRemain, claRemain = $claRemain)"
  }
  class MapMaster extends Achievement {
    val maps = MapsToComplete.maps.map(m => m -> new MapCompletionAchievement(m)).toMap
    val target = maps.size
    def completed = maps.count(_._2.isCompleted)
    def remaining = target - completed
    def isCompleted = remaining == 0
    def progress = completed
    override def toString =
      s"""MapMaster(completed = $isCompleted, remaining = $remaining, target = $target, maps = $maps)"""
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
    var timePlayed: Int = 0,
    var gamesPlayed: Int = 0,
    var fiftyGamesEarned: Int = 0,
    var twentyHoursPlayedEarned: Int = 0,
    var fiftyFlagsEarned: Int = 0,
    var tenGamesInADayEarned: Int = 0,
    var thousandFragsEarned: Int = 0,
    var isMapMaster: Boolean = false,
    var socialiteEarned: Int = 0,
    val tenGames: TenGamesInADayAchievement = new TenGamesInADayAchievement(),
    val twentyHours: TwentyHoursPlayedAchievement = new TwentyHoursPlayedAchievement(0),
    val thousandFrags: ThousandFragsAchievement = new ThousandFragsAchievement(0),
    val fiftyFlags: FiftyFlagsAchievement = new FiftyFlagsAchievement(0),
    val fiftyGames: FiftyGamesAchievement = new FiftyGamesAchievement(0),
    val mapMaster: MapMaster = new MapMaster
  ) {
    val socialite = new Socialite(id)
    override def toString =
      s"""User(id = $id, twentyHoursPlayedEarned = $twentyHoursPlayedEarned, gamesPlayed = $gamesPlayed, flags = $flags, frags = $frags, timePlayed = $timePlayed, tenGamesInADayEarned = $tenGamesInADayEarned, thousandFragsEarned = $thousandFragsEarned, fiftyGamesEarned = $fiftyGamesEarned, fiftyFlagsEarned = $fiftyFlagsEarned, isMapMaster = $isMapMaster, thousandFrags = $thousandFrags, fiftyFlags = $fiftyFlags, fiftyGames = $fiftyGames, tenGames = $tenGames, twentyHours = $twentyHours, mapMaster = $mapMaster"""

    def toXml =
      <user-record
        id={s"$id"}
        flags={s"$flags"}
      >
        <counts flags={s"$flags"} frags={s"$frags"} games={s"$gamesPlayed"} time={s"PT${timePlayed}M"}/>
        <achievements>
          <twenty-hours-played times={s"$twentyHoursPlayedEarned"} progress={twentyHours.progress.toString} target={twentyHours.target.toString} remaining-to-next={twentyHours.remaining.toString}/>
          <fifty-games-earned times={s"$fiftyGamesEarned"} progress={fiftyGames.progress.toString} target={fiftyGames.target.toString} remaining-to-next={fiftyGames.remaining.toString}/>
          <fifty-flags-earned times={fiftyFlagsEarned.toString} progress={fiftyFlags.progress.toString} target={fiftyFlags.target.toString} remaining-to-next={fiftyFlags.remaining.toString}/>
          <ten-games-in-a-day times={tenGamesInADayEarned.toString}/>
          <thousand-frags-earned times={thousandFragsEarned.toString}  progress={thousandFrags.progress.toString} target={thousandFrags.target.toString} remaining-to-next={thousandFrags.remaining.toString}/>
          <map-master earned={if ( isMapMaster) "earned" else null }
                      progress={if(isMapMaster) null else {mapMaster.progress.toString}}
                      remaining={if(isMapMaster) null else {mapMaster.remaining.toString}}
                      target={if(isMapMaster) null else {mapMaster.target.toString}}
          >
            {
            for { (map, mapCompletion) <- mapMaster.maps.toList }
              yield <map-completion
              mode={map.mode}
              map={map.name}
              is-completed={mapCompletion.isCompleted.toString}
              progress-cla={if (mapCompletion.isCompleted) null else mapCompletion.claProgress.toString}
              progress-rvsf={if (mapCompletion.isCompleted) null else mapCompletion.rvsfProgress.toString}
              target-cla={if (mapCompletion.isCompleted) null else mapCompletion.claTarget.toString}
              target-rvsf={if (mapCompletion.isCompleted) null else mapCompletion.rvsfTarget.toString}
              remaining-cla={if (mapCompletion.isCompleted) null else mapCompletion.claRemain.toString}
              remaining-rvsf={if (mapCompletion.isCompleted) null else mapCompletion.rvsfRemain.toString}
              />
            }
          </map-master>
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
  case object ScoredFiftyFlags extends UserEvent {
    override def asXml = <scored-fifty-flags-event/>
  }
  case object BecameMapMaster extends UserEvent {
    override def asXml = <became-map-master-event/>
  }
  case object PlayedTwentyHours extends UserEvent {
    override def asXml = <played-twenty-hours-event/>
  }
  case object PlayedFiftyGames extends UserEvent {
    override def asXml = <played-fifty-games-event/>
  }
  case object PlayedTenGamesInADay extends UserEvent {
    override def asXml = <played-ten-games-in-a-day-event/>
  }
  case object PlayedFiveDaysInARow extends UserEvent {
    override def asXml = <played-five-days-in-a-row-event/>
  }
  case object EarnedThousandFrags extends UserEvent {
    override def asXml = <earned-thousand-frags-event/>
  }
  case class CompletedMap(acMap: AcMap) extends UserEvent {
    override def asXml = <completed-map-event mode={acMap.mode} map={acMap.name}/>
  }
  type EmmittedEvents[UserId] = collection.immutable.Set[(UserId, UserEvent)]
  type UserRepository[UserId] = Player => Option[User[UserId]]
  def acceptGame[UserId](userRepository: UserRepository[UserId])(game: Game): EmmittedEvents[UserId] = {

    val events = scala.collection.mutable.Buffer.empty[(UserId, UserEvent)]

    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
    } {
      for { addFlags <- player.flags }
      { user.flags = user.flags + addFlags }
      user.frags = user.frags + player.frags
      user.timePlayed = user.timePlayed + game.duration
      user.gamesPlayed = user.gamesPlayed + 1
    }

    // the twenty hours achievement
    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
    } {
      user.twentyHours.duration = user.twentyHours.duration + game.duration
      if ( user.twentyHours.isCompleted ) {
        user.twentyHoursPlayedEarned = user.twentyHoursPlayedEarned + 1
        user.twentyHours.duration = 0
        events += userId -> PlayedTwentyHours
      }
    }

    // ten games in a day achievement
    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
    } {
      user.tenGames.include(game.date.substring(0,10))
      if ( user.tenGames.isCompleted ) {
        user.tenGames.counter = 0
        user.tenGamesInADayEarned = user.tenGamesInADayEarned + 1
        events += userId -> PlayedTenGamesInADay
      }
    }

    // fifty games achievement
    for {
      team <- game.teams
      player <- team.players
      user <- userRepository(player)
      fifty = user.fiftyGames
    } {
      fifty.games = fifty.games + 1
      if ( fifty.isCompleted ) {
        user.fiftyGamesEarned = user.fiftyGamesEarned + 1
        fifty.games = 0
        events += user.id -> PlayedFiftyGames
      }
    }
    // fifty flags achievement
    if ( game.acMap.mode == "ctf" ) {
      for {
        (userId, user, player) <- game.playersAsUsers(userRepository)
        flags <- player.flags
        fifty = user.fiftyFlags
      } {
        fifty.include(flags)
        if ( fifty.isCompleted ) {
          user.fiftyFlagsEarned = user.fiftyFlagsEarned + 1
          fifty.flags = fifty.overflow
          fifty.overflow = 0
          events += userId -> ScoredFiftyFlags
        }
      }
    }
    // thousand frags achievement
    for {
      (userId, user, player) <- game.playersAsUsers(userRepository)
    } {
      user.thousandFrags.include(player.frags)
      if ( user.thousandFrags.isCompleted ) {
        user.thousandFragsEarned = user.thousandFragsEarned + 1
        user.thousandFrags.frags = user.thousandFrags.overflow
        user.thousandFrags.overflow = 0
        events += userId -> EarnedThousandFrags
      }
    }
    // map completion achievement
    for {
      team <- game.teams
      player <- team.players
      user <- userRepository(player)
      acMap = game.acMap
      mapCompletion <- user.mapMaster.maps get acMap
      if !mapCompletion.isCompleted
    } {
      if ( team.name == "RVSF" && mapCompletion.rvsfRemain > 0 ) {
        mapCompletion.rvsfRemain = mapCompletion.rvsfRemain - 1
      } else if ( team.name == "CLA" && mapCompletion.claRemain > 0 ) {
        mapCompletion.claRemain = mapCompletion.claRemain - 1
      }
      if ( mapCompletion.isCompleted ) {
        events += user.id -> CompletedMap(acMap)
      }
      if ( mapCompletion.isCompleted && user.mapMaster.isCompleted ) {
        user.isMapMaster = true
        events += user.id -> BecameMapMaster
        // we can trigger side effects here, or at least return new achievements and stuff.
      }
    }
    (for {
      (userId, event) <- events
    } yield (userId, event)).toSet
  }

}
