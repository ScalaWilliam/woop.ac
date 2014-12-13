package acleague.actors

import java.util.Date

import acleague.actors.SyslogServerActor.SyslogServerOptions
import acleague.syslog.SyslogServerEventIFScala
import akka.actor.{ActorLogging, Props, ActorSystem, ActorRef}
import org.productivity.java.syslog4j.server._

object SyslogServerActor {

  case class SyslogServerEventIFScala(facility: Int, date: Option[Date], level: Int, host: Option[String], message: String)

  object SyslogServerEventIFScala {
    def apply(event: SyslogServerEventIF): SyslogServerEventIFScala = {
      SyslogServerEventIFScala(
        facility = event.getFacility,
        date = Option(event.getDate),
        level = event.getLevel,
        host = Option(event.getHost),
        message = event.getMessage
      )
    }
  }

  case class SyslogServerOptions(protocol: String, host: String, port: Int)
  def props(syslogServerOptions: SyslogServerOptions) = Props(new SyslogServerActor(syslogServerOptions))

}

import akka.actor.ActorDSL._

/** Don't really need an actor, but we'll have one anyway **/

class SyslogServerActor(syslogServerOptions: SyslogServerOptions) extends Act with ActorLogging {

  val syslogserver = SyslogServer.getInstance(syslogServerOptions.protocol)
  syslogserver.getConfig.setPort(syslogServerOptions.port)
  syslogserver.getConfig.setHost(syslogServerOptions.host)
  val handler = new SyslogServerEventHandlerIF {
    override def event(syslogServer: SyslogServerIF, event: SyslogServerEventIF): Unit = {
      val scalaEvent = SyslogServerEventIFScala(event)
      log.debug("Received event from syslog server {}", scalaEvent)
      context.system.eventStream.publish(scalaEvent)
    }
  }
  syslogserver.getConfig.addEventHandler(handler)

  whenStarting {
    log.info("Starting Syslog Server with options {}", syslogServerOptions)
    syslogserver.run()
  }

  whenStopping {
    syslogserver.shutdown()
  }

}
