package acleague.actors

import java.io.{File, FileOutputStream}
import java.util.Date
import ReceiveMessages.RealMessage
import akka.actor.ActorDSL._
import akka.actor.{ActorLogging, Props}

class FileJournallingActor(to: File) extends Act with ActorLogging {
  val os = new FileOutputStream(to, true)
  whenStarting {
    log.info(s"File Journaler started. Appending to: $to")
  }
  become {
    case message @ RealMessage(date, serverName, payload) =>
      log.debug("Received real message {}", message)
      os.write(s"""Date: $date, Server: $serverName, Payload: $payload\n""".getBytes)
  }
}

object FileJournallingActor {
  def localProps(file: File) = Props(new FileJournallingActor(file))
}