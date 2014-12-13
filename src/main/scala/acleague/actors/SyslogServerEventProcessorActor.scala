package acleague.actors

import java.util.Date

import acleague.actors.ReceiveMessages.RealMessage
import acleague.syslog.SyslogServerEventIFScala
import akka.actor.ActorDSL._
import akka.actor.{ActorRef, ActorLogging, Props}

object SyslogServerEventProcessorActor {
  def props = Props(new SyslogServerEventProcessorActor)
}

class SyslogServerEventProcessorActor extends Act with ActorLogging {
  whenStarting {
    log.info("Starting Syslog Server event processor")
  }
  val registeredServers = scala.collection.mutable.Map.empty[String, ActorRef]
  become {
    case receivedEvent @ SyslogServerEventIFScala(_, date, _, host, message) =>
      log.debug("Server event processor received: {}", receivedEvent)
      val fullMessage = host.map(h => s"$h ").getOrElse("") + message
      registeredServers.find(s => fullMessage.startsWith(s._1)) match {
        case Some((foundServer, actor)) =>

          val minN = foundServer.length + 2
          val contained = if ( fullMessage.length >= minN ) {
            val actualMessage = fullMessage.substring(minN)
            RealMessage(date.getOrElse(new Date), foundServer, actualMessage)
          } else {
            RealMessage(date.getOrElse(new Date), foundServer, "")
          }
          context.system.eventStream.publish(contained)
          actor ! contained

        case None =>
          val matcher = """(.*): Status at [^ ]+ [^ ]+: \d+.*""".r
          fullMessage match {
            case matcher(serverId) =>
              log.info("Registered new server: '{}'", serverId)
              registeredServers += serverId -> context.actorOf(IndividualServerActor.props(serverId))
            case other =>
              // ignore - looks like another service
          }
      }
  }
}