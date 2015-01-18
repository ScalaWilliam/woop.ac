package acleague.ranker.actors

import acleague.ranker.achievements.Imperative
import Imperative.{Game, User, UserEvent}
import acleague.ranker.actors.RankerSecond.{ProcessedGame, FoundEvent, UpdatedUser, NewGameFound}
import acleague.ranker.{achievements, LookupRange}
import LookupRange.IpRangeOptionalCountry
import acleague.ranker.actors.RankerActor.RegisteredUser
import akka.actor.ActorDSL._
import akka.actor._
import scala.xml.Elem

/** Actor not completely necessary, but we might want to have access to this data nonetheless **/
class RankerSecond(ipDatabase: Set[IpRangeOptionalCountry], registeredUsers: Set[RegisteredUser]) extends Act with ActorLogging {

  val nicknameToCountryUser = {
    for {RegisteredUser(nickname, id, name, countryCode) <- registeredUsers}
    yield nickname -> (countryCode, new User[String](id))
  }.toMap

  val users = {
    for { (nickname, (countryCode, user)) <- nicknameToCountryUser }
    yield user.id -> user
  }

  def ipToCountryCode(ip: String): Option[String] = {
    ipDatabase.find(_.ipIsInRange(ip)).flatMap(_.optionalCountryCode)
  }

  def userLookup(player: Imperative.Player): Option[Imperative.User[String]] = {
    for {
      (countryCode, user) <- nicknameToCountryUser.get(player.name)
      if player.host.nonEmpty
      playerCountryCode <- ipToCountryCode(player.host)
      if playerCountryCode == countryCode
    } yield user
  }

  become {
    case NewGameFound(game) =>
      log.debug("Received new game, game ID {}", game.id)
      val acceptanceResult = achievements.Imperative.acceptGame(userLookup)(game)
      log.debug("Game ID {} produced {} user changes and {} events", game.id, acceptanceResult.affectedUsers.size, acceptanceResult.emmittedEvents.size)
      for {
        userId <- acceptanceResult.affectedUsers
        user <- users.get(userId)
        updatedUser = UpdatedUser(userId = userId, xml = user.toXml)
      } { context.parent ! updatedUser }

      for {
        (userId, userEvent) <- acceptanceResult.emmittedEvents
        event = FoundEvent(gameId = game.id, userId = userId, event = userEvent)
      } { context.parent ! event }

      context.parent ! ProcessedGame(game.id)
  }

}

object RankerSecond {
  def props(ipDatabase: Set[IpRangeOptionalCountry], registeredUsers: Set[RegisteredUser]) =
    Props(new RankerSecond(ipDatabase, registeredUsers))
  case class ProcessedGame(gameId: String)
  case class FoundEvent(gameId: String, userId: String, event: UserEvent) {
    def toXml = <user-event at-game={gameId} user-id={userId}>{event.asXml}</user-event>
  }
  case class UpdatedUser(userId: String, xml: Elem)
  case class NewGameFound(game: Game)
  case class Nickname(nickname: String)
  case class IP(ip: String)
  case class CountryCode(countryCode: String)
  case class Options(getUser: Nickname => IP => Option[Username])
  case class Username(username: String)
}