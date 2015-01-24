package acleague.ranker.app

import acleague.ranker.achievements.Imperative
import acleague.ranker.achievements.Imperative.User
import acleague.ranker.actors.RankerActor
import acleague.ranker.actors.RankerActor.RegisteredUser
import acleague.ranker.actors.RankerSecond.{FoundEvent, UpdatedUser}
import org.joda.time.{DateTimeComparator, DateTime}

object LocalSynchronousTesterApp extends App {
  // no side effects: we'll only load the achievements
  val ipDatabase = RankerActor.getRanges.toList
  val registeredUsers = RankerActor.getUsers.toList

  val registeredUsersUser = registeredUsers.map(u => u -> new User[String](u.id)).toMap
  val users = registeredUsersUser.map(x => x._1.id -> x._2).toMap

  def ipToCountryCode(ip: String): Option[String] = {
    ipDatabase.find(_.ipIsInRange(ip)).flatMap(_.optionalCountryCode)
  }
  val comparator = DateTimeComparator.getInstance

  // todo implement lru cache
  def userLookup(date: DateTime)(player: Imperative.Player): Option[Imperative.User[String]] = {
    for {
      (ru, user) <- registeredUsersUser
      if player.host.nonEmpty
      nick <- ru.nicknames
      if player.name == nick.nickname
      if comparator.compare(nick.from, date) == -1
      if nick.to.isEmpty || nick.to.exists(toDate => comparator.compare(toDate, date) == 1)
      playerCountryCode <- ipToCountryCode(player.host)
      if playerCountryCode == nick.countryCode
    } yield user
  }.headOption

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
  println(users("sanzo").toXml)
//  events.filter(_.toXml.toString contains "solo").map(_.toXml).foreach(println)

}