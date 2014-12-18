package acleague.actors

import java.io.File
import java.nio.file.{StandardCopyOption, Files}
import akka.pattern.pipe
import acleague.actors.DemoDownloaderActor.DemoDownloaded
import acleague.ingesters.DemoWritten
import akka.actor.ActorDSL._
import akka.actor.{Props, ActorLogging}

import sys.process._
import java.net.{URI, URL}
import java.io.File
import scala.util.{Success, Failure, Try}

class DemoDownloaderActor(saveToDirectory: File) extends Act with ActorLogging {
  whenStarting {
    if ( !saveToDirectory.exists() ) {
      saveToDirectory.mkdirs()
    }
    log.info(s"Saving demos to: $saveToDirectory")
  }
  become {
    case gd@GameDemoFound(gameId, _, DemoWritten(filename, size)) =>
      log.info(s"Received demo to download: $gd")
      import scala.concurrent._
      import ExecutionContext.Implicits.global
      val uri = new URI(s"http://aura.woop.ac/$filename")
      val destination = new File(saveToDirectory, s"$gameId.dmo").getCanonicalFile
      Future {
        Files.copy(uri.toURL.openStream(), destination.toPath, StandardCopyOption.REPLACE_EXISTING)
        DemoDownloaded(gameId, uri, destination)
      }.onComplete {
        case Success(demoDownloaded) => self ! demoDownloaded
        case Failure(reason) =>
          log.error(s"Failed to download demo for $gd: {}", reason)
      }
    case demoDownloaded: DemoDownloaded =>
      log.info(s"Demo downloaded: $demoDownloaded")
      context.system.eventStream.publish(demoDownloaded)
  }

}
object DemoDownloaderActor {
  def props(target: File) = Props(new DemoDownloaderActor(target))
  case class DemoDownloaded(gameId: Int, source: URI, destination: File) {
  }
}