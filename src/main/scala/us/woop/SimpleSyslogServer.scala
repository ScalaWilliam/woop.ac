package us.woop
import java.util.Date
import akka.actor.{ActorSystem, ActorRef, Actor}
import org.productivity.java.syslog4j.server._
import akka.actor.ActorDSL._

class SyslogServerActor() extends Act {

}
case class SyslogServerEventIFScala(facility: Int, date: Option[Date], level: Int, host: Option[String], message: String)

object SyslogServerEventIFScala {
  def apply(event: SyslogServerEventIF): SyslogServerEventIFScala =
    SyslogServerEventIFScala(event.getFacility, Option(event.getDate), event.getLevel, Option(event.getHost), event.getMessage)
}
object SimpleSyslogServer {

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
