package acleague.app

import acleague.actors.ReceiveMessages.RealMessage
import acleague.actors.SyslogServerActor.SyslogServerOptions
import acleague.actors._
import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.publishers.GamePublisher.{NewlyAdded, ConnectionOptions}
import acleague.syslog.SyslogServerEventIFScala
import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import org.basex.BaseXServer
import org.productivity.java.syslog4j.Syslog

object LeagueApp extends App with LazyLogging {
  System.setProperty("hazelcast.logging.type", System.getProperty("hazelcast.logging.type", "slf4j"))
  logger.info(s"Application configuration: ${AppConfig.conf}")
  val server = new BaseXServer(s"-p${AppConfig.basexPort}", s"-e${AppConfig.basexPort + 1}")
  implicit val system = ActorSystem("acleague")
  val options = ConnectionOptions(
    port = AppConfig.basexPort,
    database = AppConfig.basexDatabaseName
  )
  val syslogOptions = SyslogServerOptions(
    protocol = AppConfig.syslogProtocol,
    host = AppConfig.syslogHost,
    port = AppConfig.syslogPort
  )
  def pushSyslog(options: SyslogServerOptions) = {
    val syslog = Syslog.getInstance(options.protocol)
    syslog.getConfig.setHost(options.host)
    syslog.getConfig.setPort(options.port)
    scala.io.Source.fromInputStream(getClass.getResourceAsStream("/test-run.log")).getLines().foreach(syslog.info)
  }
  val gamePublisher = system.actorOf(
    name = "gamePublisher",
    props = GamePusherActor.props(options)
  )
  val demoPublisher = system.actorOf(
    name = "demoPublisher",
    props = DemoPusherActor.props(options)
  )
  system.eventStream.subscribe(
    subscriber = demoPublisher,
    channel = classOf[GameDemoFound]
  )
  val syslogServer = system.actorOf(
    name = "syslogServer",
    props = SyslogServerActor.props(syslogOptions)
  )
  val syslogProcessor = system.actorOf(
    name = "syslogProcessor",
    props = SyslogServerEventProcessorActor.props
  )
  system.eventStream.subscribe(
    subscriber = syslogProcessor,
    channel = classOf[SyslogServerEventIFScala]
  )
  system.eventStream.subscribe(
    subscriber = gamePublisher,
    channel = classOf[GameXmlReady]
  )
  if ( AppConfig.journalEnable ) {
    val journaller = system.actorOf(
      name = "fileJournaler",
      props = FileJournallingActor.localProps(AppConfig.journalPath)
    )
    system.eventStream.subscribe(
      subscriber = journaller,
      channel = classOf[RealMessage]
    )
  }
  if ( AppConfig.hazelcastEnable ) {
    val hazelcastInstance = system.actorOf(
      name = "hazelcastInstance",
      props = HazelcastPublisherActor.props(topicName = AppConfig.hazelcastGameTopicName, demoTopicName = AppConfig.hazelcastDemoTopicName)
    )
    system.eventStream.subscribe(
      subscriber = hazelcastInstance,
      channel = classOf[NewlyAdded]
    )
    system.eventStream.subscribe(
      subscriber = hazelcastInstance,
      channel = classOf[GameDemoFound]
    )
  }

  if ( AppConfig.diagnosticEnable ) {
    Thread.sleep(1000)
    pushSyslog(syslogOptions)
  }

  system.awaitTermination()
  server.stop()
}
