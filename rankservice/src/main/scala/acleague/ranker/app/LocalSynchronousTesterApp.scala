package acleague.ranker.app

import acleague.ranker.achievements.Imperative
import acleague.ranker.achievements.Imperative.User
import acleague.ranker.actors.RankerActor
import acleague.ranker.actors.RankerActor.RegisteredUser
import acleague.ranker.actors.RankerSecond.{FoundEvent, UpdatedUser}

object LocalSynchronousTesterApp extends App {
  // no side effects: we'll only load the achievements
  val ipDatabase = RankerActor.getRanges.toList
  val registeredUsers = RankerActor.getUsers.toList

  val nicknameToCountryUser = {
    for {RegisteredUser(nickname, id, name, countryCode) <- registeredUsers}
    yield nickname ->(countryCode, new User[String](id))
  }.toMap

  val users = {
    for {(nickname, (countryCode, user)) <- nicknameToCountryUser}
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

  val (_, _, games) = RankerActor.getGames(limit = 50000)
  val events = scala.collection.mutable.ArrayBuffer.empty[FoundEvent]
  for {game <- games} {
    val acceptanceResult = Imperative.acceptGame(userLookup)(game)
    for {
      userId <- acceptanceResult.affectedUsers
      user <- users.get(userId)
      updatedUser = UpdatedUser(userId = userId, xml = user.toXml)
    } println(s"game ${game.id} updated user: $userId:  $user")

    for {
      (userId, userEvent) <- acceptanceResult.emmittedEvents
      event = FoundEvent(gameId = game.id, userId = userId, event = userEvent)
    } {
      events += event
      println(s"game ${game.id} caused event for user: $userId: $event")
    }
  }

//  println(users("lucas").toXml)
//  println(users("s4m").toXml)
  events.filter(_.toXml.toString contains "solo-flag").map(_.toXml).foreach(println)

}