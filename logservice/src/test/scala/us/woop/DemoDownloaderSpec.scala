package us.woop

import java.io.File
import java.nio.file.Files
import acleague.actors.DemoDownloaderActor.DemoDownloaded
import acleague.actors.{DemoDownloaderActor, GameDemoFound, SyslogServerEventProcessorActor}
import acleague.enrichers.EnrichFoundGame.GameXmlReady
import acleague.ingesters.DemoWritten
import acleague.syslog.SyslogServerEventIFScala
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, DefaultTimeout, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class DemoDownloaderSpec
  extends TestKit(ActorSystem("TestKitUsageSpec"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    shutdown()
  }

  "Demo downloader" should {
    "Download stuff for Aura: Current" in {
      system.eventStream.subscribe(testActor, classOf[DemoDownloaded])
      val tempDirectory = Files.createTempDirectory("demos").toFile
      tempDirectory.mkdir()
      try {
        val yay = system.actorOf(DemoDownloaderActor.props(tempDirectory), "goody")
        yay ! GameDemoFound(1234, "aura", null, DemoWritten("demos/1999/20150102_1135_local_ac_desert_8min_DM.dmo", "whatever"))
        import concurrent.duration._
        val downloadedMsg = expectMsgClass(10.seconds, classOf[DemoDownloaded])
        downloadedMsg.destination.exists() shouldBe true
        println(downloadedMsg)
      } finally {
        tempDirectory.delete()
      }
    }
    "Download stuff for Tyr: Current" in {
      system.eventStream.subscribe(testActor, classOf[DemoDownloaded])
      val tempDirectory = Files.createTempDirectory("demos").toFile
      tempDirectory.mkdir()
      try {
        val yay = system.actorOf(DemoDownloaderActor.props(tempDirectory), "goodyB")
        yay ! GameDemoFound(1234, "tyr", null, DemoWritten("/home/tyr/ac/demos/1999/20150117_2210_local_ac_shine_12min_CTF.dmo", "whatever"))
        import concurrent.duration._
        val downloadedMsg = expectMsgClass(10.seconds, classOf[DemoDownloaded])
        downloadedMsg.destination.exists() shouldBe true
        println(downloadedMsg)
      } finally {
        tempDirectory.delete()
      }
    }
    "Download stuff for Aura: Possibly" in {
      system.eventStream.subscribe(testActor, classOf[DemoDownloaded])
      val tempDirectory = Files.createTempDirectory("demos").toFile
      tempDirectory.mkdir()
      try {
        val yay = system.actorOf(DemoDownloaderActor.props(tempDirectory), "goodyC")
        yay ! GameDemoFound(1234, "aura", null, DemoWritten("/home/assaultcube/demos/1999/20150102_1135_local_ac_desert_8min_DM.dmo", "whatever"))
        import concurrent.duration._
        val downloadedMsg = expectMsgClass(10.seconds, classOf[DemoDownloaded])
        downloadedMsg.destination.exists() shouldBe true
        println(downloadedMsg)
      } finally {
        tempDirectory.delete()
      }
    }
    "Fail stuff" in {
      system.eventStream.subscribe(testActor, classOf[DemoDownloaded])
      val tempDirectory = Files.createTempDirectory("demos").toFile
      tempDirectory.mkdir()
      try {
        tempDirectory.listFiles().toList shouldBe empty
        val yay = system.actorOf(DemoDownloaderActor.props(tempDirectory), "goodye")
        yay ! GameDemoFound(1234, "aura", null, DemoWritten("demos/201412dwwqdw16_1638_local_ac_aqqueous_2min_TDM.dmo", "whatever"))
        import concurrent.duration._
        expectNoMsg(10.seconds)
        tempDirectory.listFiles().toList shouldBe empty
      } finally {
        tempDirectory.delete()
      }
    }
  }

}
