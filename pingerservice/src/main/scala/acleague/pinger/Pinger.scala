package acleague.pinger

import java.net.InetSocketAddress
import acleague.pinger.Pinger._
import acleague.pinger.PongParser._
import akka.actor.Props
import akka.io.{Udp, IO}
import akka.util.ByteString
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat

object Pinger {

  val modes = List(
    "team deathmatch", "coopedit", "deathmatch", "survivor",
    "team survivor", "ctf", "pistol frenzy", "bot team deathmatch", "bot deathmatch", "last swiss standing",
    "one shot, one kill", "team one shot, one kill", "bot one shot, one kill", "hunt the flag", "team keep the flag",
    "keep the flag", "team pistol frenzy", "team last swiss standing", "bot pistol frenzy", "bot last swiss standing", "bot team survivor", "bot team one shot, one kill"
  ).zipWithIndex.map(_.swap).toMap

  case class SendPings(ip: String, port: Int)

  trait ServerStateMachine {
    def next(input: ParsedResponse): ServerStateMachine
  }

  val teamModes = Set(0, 4, 5, 7, 11, 13, 14, 16, 17, 20, 21)

  case object NothingServerStateMachine extends ServerStateMachine {
    override def next(input: ParsedResponse) = PartialServerStateMachine().next(input)
  }

  case class PartialServerStateMachine(serverInfoReplyO: Option[ServerInfoReply] = None,
                                       playerCnsO: Option[PlayerCns] = None,
                                       playerInfoReplies: List[PlayerInfoReply] = List.empty,
                                       teamInfosO: Option[TeamInfos] = None) extends ServerStateMachine {
    override def next(input: ParsedResponse) = {
      val nextResult = input match {
        case p: PlayerCns =>
          this.copy(playerCnsO = Option(p))
        case p: PlayerInfoReply if playerCnsO.toSeq.flatMap(_.cns).contains(p.clientNum) =>
          this.copy(playerInfoReplies = playerInfoReplies :+ p)
        case s: ServerInfoReply =>
          this.copy(serverInfoReplyO = Option(s))
        case ts: TeamInfos =>
          this.copy(teamInfosO = Option(ts))
      }
      nextResult match {
        case PartialServerStateMachine(Some(serverInfo), Some(PlayerCns(cns)), playerInfos, Some(teamInfos)) if playerInfos.size == cns.size =>
          CompletedServerStateMachine(serverInfo, playerInfos, Option(teamInfos))
        case PartialServerStateMachine(Some(serverInfo), Some(PlayerCns(cns)), playerInfos, None) if cns.nonEmpty && playerInfos.size == cns.size && !teamModes.contains(serverInfo.mode) =>
          CompletedServerStateMachine(serverInfo, playerInfos, None)
        case other => other
      }
    }
  }

  case class GotParsedResponse(from: (String, Int), stuff: ParsedResponse)

  object GotParsedResponse {
    def apply(inetSocketAddress: InetSocketAddress, stuff: ParsedResponse): GotParsedResponse = {
      GotParsedResponse((inetSocketAddress.getAddress.getHostAddress, inetSocketAddress.getPort - 1), stuff)
    }
  }

  case class ServerStatus(server: String, connectName: String, shortName: String, description: String, maxClients: Int, updatedTime: String, game: Option[CurrentGame]) {
    def toJson = {
      import org.json4s._
      import org.json4s.JsonDSL._
      import org.json4s.jackson.Serialization
      import org.json4s.jackson.Serialization.{read, write}
      implicit val formats = Serialization.formats(NoTypeHints)
      write(this)
    }
  }

  case class CurrentGame(mode: String, map: String, minRemain: Int, numClients: Int, teams: Option[Map[String, ServerTeam]], players: Option[List[ServerPlayer]])

  case class ServerTeam(flags: Option[Int], frags: Int, players: List[ServerPlayer])

  case class ServerPlayer(name: String, ping: Int, frags: Int, flags: Option[Int], isAdmin: Boolean, state: String, ip: String)

  case class CurrentGameStatus(when: String = "right now", reasonablyActive: Boolean, now: CurrentGameNow, hasFlags: Boolean, map: Option[String], mode: Option[String], minRemain: Int, teams: List[CurrentGameTeam], updatedTime: String, players: Option[List[String]]) {
    def toJson = {
      import org.json4s._
      import org.json4s.JsonDSL._
      import org.json4s.jackson.Serialization
      import org.json4s.jackson.Serialization.{read, write}
      implicit val formats = Serialization.formats(NoTypeHints)
      write(this)
    }
  }
  case class CurrentGameTeam(name: String, flags: Option[Int], frags: Int, players: List[CurrentGamePlayer])
  case class CurrentGamePlayer(name: String, flags: Option[Int], frags: Int)
  case class CurrentGameNow(server: CurrentGameNowServer)
  case class CurrentGameNowServer(server: String, connectName: String, shortName: String, description: String)

  val playerStates = List("alive", "dead", "spawning", "lagged", "editing", "spectate").zipWithIndex.map(_.swap).toMap
  val guns = List("knife", "pistol", "carbine", "shotgun", "subgun", "sniper", "assault", "cpistol", "grenade", "pistol").zipWithIndex.map(_.swap).toMap

  val connects = Map("62.210.131.155" -> "aura.woop.ac", "104.219.54.14" -> "tyr.woop.ac")
  val shortName = Map("62.210.131.155" -> "Aura", "104.219.54.14" -> "Tyr")

  case class CompletedServerStateMachine(serverInfoReply: ServerInfoReply, playerInfoReplies: List[PlayerInfoReply], teamInfos: Option[TeamInfos]) extends ServerStateMachine {
    override def next(input: ParsedResponse) = NothingServerStateMachine.next(input)

    def toGameNow(ip: String, port: Int) =
      CurrentGameStatus(
        updatedTime = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.forID("UTC")).print(System.currentTimeMillis()),
        now = CurrentGameNow(
          server = CurrentGameNowServer(
            server = s"$ip:$port",
            connectName = connects.getOrElse(ip, ip) + s" $port",
            shortName = shortName.getOrElse(ip, ip) + s" $port",
            description = serverInfoReply.desc.replaceAll( """\f\d""", "")
          )
        ),
        reasonablyActive = serverInfoReply.mapName.nonEmpty && teamInfos.nonEmpty && playerInfoReplies.size >= 2,
        hasFlags = playerInfoReplies.exists(_.flagScore >= 0),
        map = Option(serverInfoReply.mapName).filter(_.nonEmpty),
        mode = modes.get(serverInfoReply.mode),
        minRemain = serverInfoReply.minRemain,
        players = if ( teamInfos.nonEmpty ) None else Option(playerInfoReplies.map(_.name)),
        teams = (for {
          TeamScore(name, frags, flags) <- teamInfos.toSeq.flatMap(_.teams)
        } yield CurrentGameTeam(
            name = name,
            flags = Option(flags).filter(_ >= 0),
            frags = frags,
          players = for {
            p <- playerInfoReplies.sortBy(x => (x.flagScore, x.frags)).reverse
            if p.team == name
          } yield CurrentGamePlayer(name = p.name, flags = Option(p.flagScore).filter(_>=0), frags = p.frags)
          )).toList
      )

    def toStatus(ip: String, port: Int): ServerStatus = {
      ServerStatus(
        server = s"$ip:$port",
      connectName = connects.getOrElse(ip, ip) + s" $port",
      shortName = shortName.getOrElse(ip, ip) + s" $port",
        description = serverInfoReply.desc.replaceAll( """\f\d""", ""),
        updatedTime = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.forID("UTC")).print(System.currentTimeMillis()),
        maxClients = serverInfoReply.maxClients,
        game = for {
          mode <- modes.get(serverInfoReply.mode)
          if serverInfoReply.mapName.nonEmpty
          if serverInfoReply.numPlayers > 0
          teamsO = for {
            TeamInfos(_, _, teams) <- teamInfos
            teamsList = for {TeamScore(name, frags, flags) <- teams} yield name -> ServerTeam(Option(flags).filter(_ >= 0), frags, players = {
              for {p <- playerInfoReplies
              if p.team == name}
              yield ServerPlayer(p.name, p.ping, p.frags, Option(p.flagScore).filter(_ > 0), isAdmin = p.role == 1,
                playerStates.getOrElse(p.state, "unknown"), p.ip)
            })
          } yield teamsList.toMap
          playersO = if (teamInfos.nonEmpty) None
          else Option {
            for {p <- playerInfoReplies}
            yield ServerPlayer(p.name, p.ping, p.frags, Option(p.flagScore).filter(_ >= 0), isAdmin = p.role == 1, playerStates.getOrElse(p.state, "unknown"), p.ip)
          }
        } yield CurrentGame(mode, serverInfoReply.mapName, serverInfoReply.minRemain, serverInfoReply.numPlayers, teamsO, playersO)
      )
    }
  }
  def props = Props(new Pinger)
}

import akka.actor.ActorDSL._

class Pinger extends Act {
  val serverStates = scala.collection.mutable.Map.empty[(String, Int), ServerStateMachine].withDefaultValue(NothingServerStateMachine)
  whenStarting {
    import context.system
    IO(Udp) ! Udp.Bind(self, new InetSocketAddress("0.0.0.0", 0))
  }

  become {
    case Udp.Bound(boundTo) =>
      val udp = sender()
      import PongParser.>>:
      become {
        case Udp.Received(PongParser.GetInt(1, PongParser.GetServerInfoReply(stuff)), from) =>
          self ! GotParsedResponse(from, stuff)
        case Udp.Received(0 >>: 1 >>: _ >>: PongParser.GetPlayerCns(stuff), from) =>
          self ! GotParsedResponse(from, stuff)
        case Udp.Received(0 >>: 1 >>: _ >>: PongParser.GetPlayerInfos(stuff), from) =>
          self ! GotParsedResponse(from, stuff)
        case Udp.Received(0 >>: 2 >>: _ >>: PongParser.GetTeamInfos(stuff), from) =>
          self ! GotParsedResponse(from, stuff)
        case GotParsedResponse(from, stuff) =>
          val nextState = serverStates(from).next(stuff)
          serverStates += from -> nextState
          nextState match {
            case r: CompletedServerStateMachine =>
              val newStatus = r.toStatus(from._1, from._2)
              context.system.eventStream.publish(newStatus)
              val newStatus2 = r.toGameNow(from._1, from._2)
              context.system.eventStream.publish(newStatus2)
            case o =>
            //                println("Not collected", from, o, stuff)
          }
        case SendPings(ip, port) =>
          val socket = new InetSocketAddress(ip, port + 1)
          udp ! Udp.Send(ByteString(1), socket)
          udp ! Udp.Send(ByteString(0, 1, 255), socket)
          udp ! Udp.Send(ByteString(0, 2, 255), socket)
      }


  }
}