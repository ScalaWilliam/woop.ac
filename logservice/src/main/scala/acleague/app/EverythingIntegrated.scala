package acleague.app

import java.io.File
import acleague.actors.{ReceiveMessages, FileJournallingActor}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

object AppConfig {
  val conf = ConfigFactory.load()
  val basexEnabled = conf.getBoolean("acleague.basex.enable")
  val basexPort = conf.getInt("acleague.basex.port")
  val basexDatabaseName = conf.getString("acleague.basex.database")
  val journalPath = new File(conf.getString("acleague.journal.path")).getCanonicalFile
  val syslogHost = conf.getString("acleague.syslog.host")
  val syslogPort = conf.getInt("acleague.syslog.port")
  val syslogProtocol = conf.getString("acleague.syslog.protocol")
  val hazelcastGameTopicName = conf.getString("acleague.hazelcast.game.topic")
  val hazelcastDemoTopicName = conf.getString("acleague.hazelcast.demo.topic")
  val hazelcastDemoDownloadTopicName = conf.getString("acleague.hazelcast.demodownload.topic")
  val hazelcastEnable = conf.getBoolean("acleague.hazelcast.enable")
  val journalEnable = conf.getBoolean("acleague.journal.enable")
  val diagnosticEnable = conf.getBoolean("acleague.diagnostic.enable")
  val downloadDemos = conf.getBoolean("acleague.demos.download.enable")
  val downloadDemosPath = new File(conf.getString("acleague.demos.download.path")).getCanonicalFile
}

object Util {
  def using[A <: { def close(): Unit }, B](subject: => A)(f: A => B): B = {
    val instance = subject
    try f(instance)
    finally instance.close()
  }
}

object EverythingIntegrated extends App with LazyLogging {
//  import Util.using
//  val uniqueId = UUID.randomUUID().toString
//  val server = new BaseXServer(s"-p${AppConfig.basexPort}",s"-e${AppConfig.basexPort + 1}")
//  using(new ClientSession("localhost", AppConfig.basexPort, "admin", "admin")) {
//    _.execute(new Check("acleague"))
//  }
//
//  val hazelcast = Hazelcast.newHazelcastInstance()
//  val topic = hazelcast.getTopic[String]("xml-games")
//
//  implicit val as = ActorSystem("Masterful")
//  val publisherActor = as.actorOf(MessagePublisher.props(sess))
//  publisherActor ! GameXmlReady(<test uuid={uniqueId}/>.toString)
//  val processor = as.actorOf(SyslogServerEventProcessorActor.props, "goody")
//  val syslogserver = SyslogServer.getInstance(syslogProtocol)
//  val journal = as.actorOf(FileJournallingActor.localProps(new File(filepath).getCanonicalFile))
////  val logger = as.actorOf(ElasticLogger.localProps, "logger")
////  as.eventStream.subscribe(logger, classOf[RealMessage])
//  as.eventStream.subscribe(journal, classOf[RealMessage])
//  val config = syslogserver.getConfig
//  config.setPort(syslogPort)
//  SimpleSyslogServer.sendToActor(config)(processor)
//  syslogserver.run()
//  server.stop()
}
