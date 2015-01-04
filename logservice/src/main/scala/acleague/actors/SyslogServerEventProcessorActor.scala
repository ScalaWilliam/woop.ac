package acleague.actors

import java.util.Date

import acleague.actors.ReceiveMessages.RealMessage
import acleague.syslog.SyslogServerEventIFScala
import akka.actor.ActorDSL._
import akka.actor.{ActorRef, ActorLogging, Props}
import org.joda.time.{LocalDateTime, DateTimeZone, DateTime}

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
          val newDate = {
            (date, foundServer) match {
              case (Some(sourceDate), serverString) if serverString contains " aura " =>
                new LocalDateTime(sourceDate).toDateTime(DateTimeZone.forID("CET"))
              case (Some(sourceDate), _) =>
                new DateTime(sourceDate, DateTimeZone.forID("UTC"))
              case _ =>
                new DateTime(DateTimeZone.forID("UTC"))
            }
          }
          val minN = foundServer.length + 2
          val contained = if ( fullMessage.length >= minN ) {
            val actualMessage = fullMessage.substring(minN)
            RealMessage(newDate, foundServer, actualMessage)
          } else {
            RealMessage(newDate, foundServer, "")
          }
          context.system.eventStream.publish(contained)
          actor ! contained

        case None =>
          val matcher = """(.*): Status at [^ ]+ [^ ]+: \d+.*""".r
/** IE
  *
[85.69.34.170] w00p|Sanzo busted .rC|xemi
[31.52.34.203] .rC|Shieldybear sprayed w00p|Sanzo
[90.35.208.230] w00p|Redbull sprayed .rC|f0rest
[31.52.34.203] .rC|Shieldybear stole the flag
[145.118.113.26] w00p|Harrek sprayed .rC|xemi
[145.118.113.26] w00p|Harrek stole the flag
            */
          val matcher2 = """(.*): \[\d+\.\d+\.\d+\.\d+\] [^ ]+ (sprayed|busted|gibbed|punctured) [^ ]+""".r
          fullMessage match {
            case matcher(serverId) =>
              log.info("Registered new server: '{}'", serverId)
              registeredServers += serverId -> context.actorOf(IndividualServerActor.props(serverId))
            case matcher2(serverId, _) =>
              log.info("Registered new server: '{}'", serverId)
              registeredServers += serverId -> context.actorOf(IndividualServerActor.props(serverId))
            case other =>
              // ignore - looks like another service
          }
      }
  }
}