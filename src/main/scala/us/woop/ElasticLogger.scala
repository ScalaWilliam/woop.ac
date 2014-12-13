package us.woop

import java.io.File

import java.io.{FileOutputStream, File}
import java.util.Date
import akka.actor.{ActorLogging, Props}
import akka.pattern._
import akka.actor.ActorDSL._
import us.woop.ReceiveMessages.RealMessage

//import akka.actor.Props
//import com.sksamuel.elastic4s.{SimpleFieldValue, ElasticClient}
//import us.woop.ReceiveMessages.RealMessage
//import com.sksamuel.elastic4s.ElasticDsl._
//class ElasticLogger(client: ElasticClient) extends Act {
//
//  client.execute(create index "logs")
//  become {
//    case message @ RealMessage(date, serverName, payload) =>
//      val newDate = date.getOrElse((new Date).toString)
//      val indexResult = client.execute {
//        index into "logs" fields(
//          "date" -> newDate,
//          "serverName" -> serverName,
//          "message" ->payload
//        )
//      }
//
////      indexResult pipeTo self
//  }
//}
//
//object ElasticLogger {
//  def localProps = Props(new ElasticLogger(ElasticClient.local))
//}

class FileLogger(to: File) extends Act with ActorLogging {
  val os = new FileOutputStream(to, true)
  log.info(s"Journalling to $to")
  become {
    case message @ RealMessage(date, serverName, payload) =>
      val realDate = date.getOrElse((new Date).toString)
      os.write(s"""Date: $realDate, Server: $serverName, Payload: $payload\n""".getBytes)
  }
}

object FileLogger {
  def localProps(file: File) = Props(new FileLogger(file))
}