package us.woop

import akka.actor.{ActorLogging, Props, ActorRef, ActorSystem}
import org.basex.core.Context
import org.basex.core.cmd.Add
import org.basex.server.ClientSession
import us.woop.CaptureGames.Parser.{FoundGame, ParserState, NothingFound}
import akka.actor.ActorDSL._
import us.woop.ReceiveMessages.RealMessage
import us.woop.ServerProcessor.GameXmlReady
import scala.xml.UnprefixedAttribute
class ServerProcessor(serverId: String) extends Act with ActorLogging {
  var state = NothingFound: ParserState
  become {
    case RealMessage(date, serverName, message) =>
      val previousState = state
      state = state.next(message)
      log.debug("[{}] {}x{} -> {}", serverName, previousState, message, state)
      state match {
        case fg @ FoundGame(header, game) =>
          val gameXml = XmlGames.foundGameXml(fg)
          val newGameXml = gameXml.copy(attributes =
            new UnprefixedAttribute("date", date.orNull,
            new UnprefixedAttribute("server", serverId,
              gameXml.attributes)
            )
          )
          context.system.eventStream.publish(GameXmlReady(s"$newGameXml"))
        case _ =>
          // ignore
      }
  }
}
object ServerProcessor {
  case class GameXmlReady(xml: String)
  def props(serverId: String) = Props(new ServerProcessor(serverId))
}
class MessageProcessor extends Act with ActorLogging {
  val registeredServers = scala.collection.mutable.Map.empty[String, ActorRef]
  become {
    case SyslogServerEventIFScala(_, date, _, host, message) =>
      val fullMessage = host.map(h => s"$h ").getOrElse("") + message
      val fromServer = registeredServers.find(s => fullMessage.startsWith(s._1))
      fromServer match {
        case Some((foundServer, actor)) =>

          val minN = foundServer.length + 2
          if ( fullMessage.length >= minN ) {
            val actualMessage = fullMessage.substring(minN)
            val contained = RealMessage(date.map(_.toString), foundServer, actualMessage)
            actor ! contained
            context.system.eventStream.publish(contained)
          }

        case None =>
          val matcher = """(.*): Status at [^ ]+ [^ ]+: \d+.*""".r
          fullMessage match {
            case matcher(serverId) =>
              log.info("Registered new server {}", serverId)
              registeredServers += serverId -> context.actorOf(ServerProcessor.props(serverId))
            case other =>
              // ignore - looks like another service
          }
      }
  }
}
object MessageProcessor {
  def props = Props(new MessageProcessor)
}

object ReceiveMessages {
  implicit val as = ActorSystem("Boom!")
  case class RealMessage(date: Option[String], serverName: String, message: String)
  /** This actor figures out "servers" from dodgy inputs basically. Should work rather well. **/
}
class MessageWriter(session: ClientSession) extends Act {
  context.system.eventStream.subscribe(self, classOf[GameXmlReady])
  become {
    case game @ GameXmlReady(xml) =>
      MessageWriter.publishMessage(session)(game)
  }
}
object MessageWriter {
  def props(sess: ClientSession) = Props(new MessageWriter(sess))
  def publishMessage(session: ClientSession)(game: GameXmlReady) = {
    val s = s"${game.xml}"
    session.execute(new Add("records", s))
  }
}