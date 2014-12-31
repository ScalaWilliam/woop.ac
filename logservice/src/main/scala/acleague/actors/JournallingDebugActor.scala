package acleague.actors

import ReceiveMessages.RealMessage
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}

class JournallingDebugActor extends Act with ActorLogging {
  become {
    case message @ RealMessage(date, serverName, payload) =>
      val fullMessage = s"""$serverName: $payload"""
      log.debug("{}", fullMessage)
  }
}

object JournallingDebugActor {
  def localProps = Props(new JournallingDebugActor)
}