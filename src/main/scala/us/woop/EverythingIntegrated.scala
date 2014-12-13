package us.woop

import java.io.File
import java.util.UUID

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import org.basex.BaseXServer
import org.basex.core.cmd.{Check, Open, CreateDB}
import org.basex.server.ClientSession
import org.productivity.java.syslog4j.server.SyslogServer
import us.woop.ReceiveMessages.RealMessage
import us.woop.ServerProcessor.GameXmlReady

import scala.util.Try
import scala.util.control.NonFatal

object EverythingIntegrated extends App with LazyLogging {
  val syslogPort = System.getProperty("syslogPort", "5000").toInt
  val syslogProtocol = System.getProperty("syslogProtocol", "tcp")
  val uniqueId = UUID.randomUUID().toString
  logger.info(s"App launching with uuid $uniqueId. BaseX port: $basexPort, Syslog port: $syslogPort ($syslogProtocol)")
  val basexPort = System.getProperty("basexPort", "1236").toInt
  val server = new BaseXServer(s"-p$basexPort",s"-e${basexPort+1}")
  def sess: ClientSession = {
    logger.info("Attempting to connect...")
    try {
      val sess = new ClientSession("localhost", basexPort, "admin", "admin")
      logger.info("Client session established")
      sess
    } catch {
      case NonFatal(e) =>
        logger.error(s"Failed to initialise client session: $e", e)
        throw e
    }
  }
  val session = (Try(sess) orElse Try(sess) orElse Try(sess)).get
  session.execute(new Check("acleague"))
  implicit val as = ActorSystem("Masterful")
  val publisherActor = as.actorOf(MessageWriter.props(session))
  publisherActor ! GameXmlReady(<test uuid={uniqueId}/>.toString)
  val processor = as.actorOf(MessageProcessor.props, "goody")
  val syslogserver = SyslogServer.getInstance(syslogProtocol)
  val journal = as.actorOf(FileLogger.localProps(new File("journal.log")))
//  val logger = as.actorOf(ElasticLogger.localProps, "logger")
//  as.eventStream.subscribe(logger, classOf[RealMessage])
  as.eventStream.subscribe(journal, classOf[RealMessage])
  val config = syslogserver.getConfig
  config.setPort(syslogPort)
  SimpleSyslogServer.sendToActor(config)(processor)
  syslogserver.run()
  server.stop()
}
