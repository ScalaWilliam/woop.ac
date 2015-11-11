package acleague.actors

import java.io.{File, FileOutputStream}

import acleague.enrichers.GameJsonFound
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}
import play.api.libs.json.JsValue

object EventsJournallingActor {
  def props(file: File) = {
    Props(new EventsJournallingActor(file))
  }
}
class EventsJournallingActor(file: File) extends Act with ActorLogging {
  val fos = new FileOutputStream(file, true)
  whenStopping {
    fos.close()
    log.info("Stopping.")
  }
  whenStarting {
    log.info("Writing to {}", file)
  }
  def push(id: String, typ: String, json: JsValue): Unit = {
    fos.write(s"$id\t$typ\t$json\n".getBytes("UTF-8"))
  }
  become {
    case GameJsonFound(jsonGame) =>
      push(jsonGame.id, "game-found", jsonGame.toJson)
  }
}
