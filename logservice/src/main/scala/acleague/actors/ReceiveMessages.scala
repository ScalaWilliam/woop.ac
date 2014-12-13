package acleague.actors

import java.util.Date

import akka.actor.ActorSystem

object ReceiveMessages {
  implicit val as = ActorSystem("Boom!")
  case class RealMessage(date: Date, serverName: String, message: String)
  /** This actor figures out "servers" from dodgy inputs basically. Should work rather well. **/
}