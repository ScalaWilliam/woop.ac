package us.woop

import acleague.actors.SyslogServerEventProcessorActor
import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.syslog.SyslogServerEventIFScala
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, DefaultTimeout, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ReceiverSpec
  extends TestKit(ActorSystem("TestKitUsageSpec"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    shutdown()
  }

  "Thingy" should {
    "Produce events after receiving some mesasges" in {
      system.eventStream.subscribe(testActor, classOf[Any])
      val yay = system.actorOf(SyslogServerEventProcessorActor.props, "goody")
      val lines = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/long-log.log")).getLines()
      for { line <- lines }
        yay ! SyslogServerEventIFScala(0, None, 0, Some("stuff"), line)
      import concurrent.duration._
      val msg = fishForMessage() { case m: GameXmlReady => true; case _ => false }
      println(msg)
    }
  }

}
