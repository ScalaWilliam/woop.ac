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
    "Download stuff" in {
      system.eventStream.subscribe(testActor, classOf[DemoDownloaded])
      val tempDirectory = Files.createTempDirectory("demos").toFile
      tempDirectory.mkdir()
      try {
        val yay = system.actorOf(DemoDownloaderActor.props(tempDirectory), "goody")
        yay ! GameDemoFound("1234", null, DemoWritten("demos/20141216_1638_local_ac_aqueous_2min_TDM.dmo", "whatever"))
        import concurrent.duration._
        val downloadedMsg = expectMsgClass(10.seconds, classOf[DemoDownloaded])
        downloadedMsg.destination.exists() shouldBe true
        println(downloadedMsg)
      } finally {
        tempDirectory.delete()
      }
    }
  }

}
