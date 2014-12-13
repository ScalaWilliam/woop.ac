package us.woop

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, DefaultTimeout, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import us.woop.ServerProcessor.GameXmlReady

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
      val yay = system.actorOf(MessageProcessor.props, "goody")
      val lines = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/long-log.log")).getLines()
      for { line <- lines }
        yay ! SyslogServerEventIFScala(0, None, 0, Some("stuff"), line)
      import concurrent.duration._
      receiveN(25)
      val msg = expectMsgClass(classOf[GameXmlReady])
      println(msg)
    }
  }

}
