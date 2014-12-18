package acleague.actors

import java.io.File
import akka.pattern.pipe
import acleague.actors.DemoDownloaderActor.DemoDownloaded
import acleague.ingesters.DemoWritten
import akka.actor.ActorDSL._
import akka.actor.{Props, ActorLogging}

import sys.process._
import java.net.{URI, URL}
import java.io.File
import scala.util.Try

class DemoDownloaderActor(saveToDirectory: File) extends Act with ActorLogging {
  whenStarting {
    if ( !saveToDirectory.exists() ) {
      saveToDirectory.mkdirs()
    }
  }
  become {
    case GameDemoFound(gameId, _, DemoWritten(filename, size)) if Try(gameId.toInt).isSuccess =>
      import scala.concurrent._
      import ExecutionContext.Implicits.global
      val uri = new URI(s"http://aura.woop.ac/$filename")
      val destination = new File(saveToDirectory, s"$gameId.dmo").getCanonicalFile
      Future {
        uri.toURL #> destination !!

        // keep the space above here
        DemoDownloaded(gameId, uri, destination)
      } pipeTo self
    case demoDownloaded: DemoDownloaded =>
      context.system.eventStream.publish(demoDownloaded)
  }

}
object DemoDownloaderActor {
  def props(target: File) = Props(new DemoDownloaderActor(target))
  case class DemoDownloaded(gameId: String, source: URI, destination: File) {
  }
}