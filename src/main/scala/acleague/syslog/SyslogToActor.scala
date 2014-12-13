package acleague.syslog

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import org.productivity.java.syslog4j.server._

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

  implicit class toDateAddition(input: String) {
    def toDate: Date = new Date(input)
  }

  def sendToActor(config: SyslogServerConfigIF)(actor: ActorRef)(implicit as: ActorSystem): SyslogServerEventHandlerIF = {

    val handler = new SyslogServerEventHandlerIF {
      override def event(syslogServer: SyslogServerIF, event: SyslogServerEventIF): Unit = {
        actor ! SyslogServerEventIFScala(event)
      }
    }

    config.addEventHandler(handler)

    handler

  }

}
