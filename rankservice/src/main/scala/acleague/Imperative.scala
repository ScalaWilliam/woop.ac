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
  class FiftyFlagsAchievement(var flags: Int, var overflow: Int = 0) extends Achievement {
    val target = 50
    def include(newFlags: Int): Unit = {
      flags = Math.min(target, flags + newFlags)
      overflow = Math.max(0, flags + newFlags - target)
    }
    def isCompleted = flags == target
    override def toString = s"FiftyFlagsAchievement(flags = $flags, overflow = $overflow)"
  }
  class FiftyGamesAchievement(var games: Int) extends Achievement {
    def isCompleted = games == 50
    override def toString = s"FiftyGamesAchievement(games = $games)"
  }
  class MapCompletionAchievement(map: AcMap, var rvsfRemain: Int = 2, var claRemain: Int = 2) extends Achievement {
    def isCompleted = rvsfRemain == 0 && claRemain == 0
    def remaining = rvsfRemain + claRemain
    def completed = 4 - remaining
    override def toString = s"MapCompletionAchievement(map = $map, rvsfRemain = $rvsfRemain, claRemain = $claRemain)"
  }
  class MapMaster extends Achievement {
    val maps = MapsToComplete.maps.map(m => m -> new MapCompletionAchievement(m)).toMap
    val target = maps.size
    def completed = maps.count(_._2.isCompleted)
    def remaining = target - completed
    def isCompleted = remaining == 0
    override def toString =
      s"""MapMaster(completed = $isCompleted, remaining = $remaining, target = $target, maps = $maps)"""
  }
  type UserId = String
  class Socialite(self: UserId) extends Achievement {
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
  class User
  (
    val id: UserId,
    var fiftyGamesEarned: Int = 0,
    var fiftyFlagsEarned: Int = 0,
    var isMapMaster: Boolean = false,
    var socialiteEarned: Int = 0,
    val fiftyFlags: FiftyFlagsAchievement = new FiftyFlagsAchievement(0),
    val fiftyGames: FiftyGamesAchievement = new FiftyGamesAchievement(0),
    val mapMaster: MapMaster = new MapMaster
  ) {
    val socialite: Socialite = new Socialite(id)
    override def toString =
      s"""User(id = $id, fiftyGamesEarned = $fiftyGamesEarned, fiftyFlagsEarned = $fiftyFlagsEarned, isMapMaster = $isMapMaster, fiftyFlags = $fiftyFlags, fiftyGames = $fiftyGames, mapMaster = $mapMaster"""
  }
  case class Player(name: String, host: String, flags: Option[Int])
  case class Team(name: String, players: Set[Player])
  case class Game(id: String, acMap: AcMap, teams: Set[Team]) {
    private def game = this
    def playersAsUsers(userRepository: UserRepository) = for {
      team <- game.teams
      player <- team.players
      userId = player.name
      user <- userRepository get userId
    } yield (userId, user, player)
  }

  type UserRepository = scala.collection.mutable.Map[String, User]

  def createGameFromXml(gameXml: Elem) = {
    Game(
      id = gameXml.attText("id"),
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
            flags = p.attribute("flags").toSeq.flatMap(x => x).map(_.text.toInt).headOption
          } yield Player(name = name, host = host, flags = flags)
          ).toSet)).toSet
    )
  }

  trait UserEvent
  case object ScoredFiftyFlags extends UserEvent
  case object BecameMapMaster extends UserEvent
  case object PlayedFiftyGames extends UserEvent
  type EmmittedEvents = collection.immutable.Set[(Game, UserId, UserEvent)]

  def acceptGame(userRepository: UserRepository)(game: Game): EmmittedEvents = {

    val events = scala.collection.mutable.Buffer.empty[(UserId, UserEvent)]

    // fifty games achievement
    for {
      team <- game.teams
      player <- team.players
      userId = player.name
      user <- userRepository get userId
      fifty = user.fiftyGames
    } {
      fifty.games = fifty.games + 1
      if ( fifty.isCompleted ) {
        user.fiftyGamesEarned = user.fiftyGamesEarned + 1
        fifty.games = 0
        events += userId -> PlayedFiftyGames
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
    // map completion achievement
    for {
      team <- game.teams
      player <- team.players
      userId = player.name
      user <- userRepository get userId
      mapCompletion <- user.mapMaster.maps get game.acMap
      if !mapCompletion.isCompleted
    } {
      if ( team.name == "RVSF" && mapCompletion.rvsfRemain > 0 ) {
        mapCompletion.rvsfRemain = mapCompletion.rvsfRemain - 1
      } else if ( team.name == "CLA" && mapCompletion.claRemain > 0 ) {
        mapCompletion.claRemain = mapCompletion.claRemain - 1
      }
      if ( mapCompletion.isCompleted && user.mapMaster.isCompleted ) {
        user.isMapMaster = true
        events += userId -> BecameMapMaster
        // we can trigger side effects here, or at least return new achievements and stuff.
      }
    }
    (for {
      (userId, event) <- events
    } yield (game, userId, event)).toSet
  }

}
