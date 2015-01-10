package acleague.actors
import acleague.actors.RankerActor._
import acleague.Imperative
import akka.actor.ActorDSL._
import akka.actor.{Props, ActorSystem, Kill}
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType

import scala.util.Try
import scala.xml.Elem

class RankerActor extends Act {

  val rankRepository = scala.collection.mutable.HashMap.empty[RegisteredUser, acleague.Imperative.User[RegisteredUser]]
  def ipToCountry(ip: String): Option[String] = Option("GB")

  def userLookup(player:acleague.Imperative.Player): Option[acleague.Imperative.User[RegisteredUser]] = {
    for {
      player_country <- ipToCountry(player.host)
      user <- rankRepository.get(RegisteredUser(player.name, player_country))
    } yield user
  }

  def fromClean(): Unit = {
    for { user <- RankerActor.getUsers } {
      rankRepository += user -> new Imperative.User[RegisteredUser](id = user)
    }
    val foundEvents =
      for {
        game <- RankerActor.getGames
        events = Imperative.acceptGame(userLookup)(game)
        if events.nonEmpty
      } yield GameEvents(game.id, events)
    pushEvents(foundEvents.toSeq)
    println("Completed")
  }

  def pushEvents(events: Seq[GameEvents]): Unit = {
    events foreach println
  }

  whenStarting {
    fromClean()
  }

  become {
    case NewUser(User(name, country)) =>
      rankRepository.clear()
      fromClean()
    case NewGame(gameId) =>
      val foundEvents = for {
        game <- RankerActor.getGame(gameId)
        events = Imperative.acceptGame(userLookup)(game)
        if events.nonEmpty
      } yield GameEvents(game.id, events)
      pushEvents(foundEvents.toSeq)
  }

}
object RankerActor {
  case class NewUser(user: User)
  case class User(name: String, country: String)
  case class NewGame(gameId: String)
  case class GameEvents(gameId: String, events: Imperative.EmmittedEvents[RegisteredUser])
  def getGames = scala.xml.XML.loadString(Request.Post("http://odin.duel.gg:1238/rest/acleague")
    .bodyString(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[
    <games>{
subsequence(
for $game in /game
let $date := xs:dateTime($game/@date) cast as xs:date
order by $date ascending
return $game, 1, 22200
)}</games>
]]></rest:text>
  </rest:query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString()).\\("game").toIterator.map(_.asInstanceOf[Elem]).map(Imperative.createGameFromXml)
  def getGame(id:String) = scala.xml.XML.loadString(Request.Post("http://odin.duel.gg:1238/rest/acleague")
    .bodyString(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[
    declare variable $id xs:string external;
    /game[@id=$id]
]]></rest:text>
    <rest:variable name="id" value={id}/>
  </rest:query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString()).\\("game").headOption.map(_.asInstanceOf[Elem]).map(Imperative.createGameFromXml)
  case class RegisteredUser(name: String, countryCode: String)
  def getUsers = for {
    xmlContent <- Option(Request.Post("http://odin.duel.gg:1238/rest/acleague")
  .bodyString(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[/registered-user
]]></rest:text>
  </rest:query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString()).toIterator
    xml <- Try(scala.xml.XML.loadString(xmlContent)).toOption.toIterator
    u <- xml.\\("registered-user").map(_.asInstanceOf[Elem]).toIterator
  } yield RegisteredUser((u\"@name").text, (u\"@country").text)

}

object RankerActorRunner extends App {
  implicit val as = ActorSystem("lel")
  val newAct = as.actorOf(Props(new RankerActor), name="wut")

  as.awaitTermination()
  as.shutdown()
}