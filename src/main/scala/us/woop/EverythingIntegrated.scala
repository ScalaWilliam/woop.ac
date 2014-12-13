package us.woop

import java.io.File

import akka.actor.ActorSystem
import org.basex.BaseXServer
import org.basex.core.cmd.{Open, CreateDB}
import org.basex.server.ClientSession
import org.productivity.java.syslog4j.server.SyslogServer
import us.woop.ReceiveMessages.RealMessage

object EverythingIntegrated extends App {
  val server = new BaseXServer("-p1236")
  val session = new ClientSession("localhost", 1236, "admin", "admin")
  session.execute(new CreateDB("acleague"))
  session.execute(new Open("acleague"))
  implicit val as = ActorSystem("Masterful")
  val publisherActor = as.actorOf(MessageWriter.props(session))
  val processor = as.actorOf(MessageProcessor.props, "goody")
  val syslogserver = SyslogServer.getInstance("tcp")
  val logger = as.actorOf(FileLogger.localProps(new File("journal.log")))
//  val logger = as.actorOf(ElasticLogger.localProps, "logger")
//  as.eventStream.subscribe(logger, classOf[RealMessage])
  as.eventStream.subscribe(logger, classOf[RealMessage])
  val config = syslogserver.getConfig
  config.setPort(5000)
  SimpleSyslogServer.sendToActor(config)(processor)
  syslogserver.run()
  server.stop()
}
