package acleague.actors

import java.util.Date

import akka.actor.ActorSystem
import org.joda.time.DateTime

object ReceiveMessages {
  implicit val as = ActorSystem("Boom!")
  case class RealMessage(date: DateTime, serverName: String, message: String)
  /** This actor figures out "servers" from dodgy inputs basically. Should work rather well. **/
}