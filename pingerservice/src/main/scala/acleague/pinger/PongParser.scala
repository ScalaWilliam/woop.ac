package acleague.pinger

import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

object PongParser extends StrictLogging {

  object GetUChar {
    def unapply(byte: Byte): Option[Int] =
      Option(byte.toChar & 0xFF)

    def unapply(int: Int): Option[Int] =
      Option(int & 0xFF)
  }

  object ##:: {
    def unapply(from: ByteString): Option[(Byte, ByteString)] =
      for { ht @ (head, tail) <- Option(from.splitAt(1))
            if head.nonEmpty } yield (head.head, tail)
  }

  object GetInt {
    def unapply(bytes: ByteString): Option[(Int, ByteString)] = bytes match {
      case -127 ##:: GetUChar(m) ##:: GetUChar(n) ##:: GetUChar(o) ##:: GetUChar(p) ##:: rest =>
        Option(((m | (n << 8)) | o << 16) | (p << 24), rest)
      case -128 ##:: GetUChar(m) ##:: n ##:: rest =>
        Option(m | (n << 8), rest)
      case n ##:: rest =>
        Option((n.toInt, rest))
      case ByteString.empty =>
        None
    }
  }

  object GetUchars {

    def uchars(bytes: ByteString): List[(Int, ByteString)] = {
      bytes match {
        case GetInt(GetUChar(value), rest) => (value, rest) :: uchars(rest)
        case ByteString.empty => Nil
      }
    }

    def unapply(bytes: ByteString): Option[List[Int]] =
      Option(uchars(bytes).map(_._1))
  }

  object GetInts {
    def ints(bytes: ByteString): List[(Int, ByteString)] = {
      bytes match {
        case GetInt(value, rest) => (value, rest) :: ints(rest)
        case ByteString.empty => Nil
      }
    }

    def unapply(bytes: ByteString): Option[List[Int]] =
      Option(ints(bytes).map(_._1))
  }

  object CubeString {
    val mapping: PartialFunction[Int, Int] = List[Int](
      0, 192, 193, 194, 195, 196, 197, 198, 199, 9, 10, 11, 12, 13, 200, 201,
      202, 203, 204, 205, 206, 207, 209, 210, 211, 212, 213, 214, 216, 217, 218, 219,
      32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
      48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
      64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
      80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95,
      96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
      112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 220,
      221, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237,
      238, 239, 241, 242, 243, 244, 245, 246, 248, 249, 250, 251, 252, 253, 255, 0x104,
      0x105, 0x106, 0x107, 0x10C, 0x10D, 0x10E, 0x10F, 0x118, 0x119, 0x11A, 0x11B, 0x11E, 0x11F, 0x130, 0x131, 0x141,
      0x142, 0x143, 0x144, 0x147, 0x148, 0x150, 0x151, 0x152, 0x153, 0x158, 0x159, 0x15A, 0x15B, 0x15E, 0x15F, 0x160,
      0x161, 0x164, 0x165, 0x16E, 0x16F, 0x170, 0x171, 0x178, 0x179, 0x17A, 0x17B, 0x17C, 0x17D, 0x17E, 0x404, 0x411,
      0x413, 0x414, 0x416, 0x417, 0x418, 0x419, 0x41B, 0x41F, 0x423, 0x424, 0x426, 0x427, 0x428, 0x429, 0x42A, 0x42B,
      0x42C, 0x42D, 0x42E, 0x42F, 0x431, 0x432, 0x433, 0x434, 0x436, 0x437, 0x438, 0x439, 0x43A, 0x43B, 0x43C, 0x43D,
      0x43F, 0x442, 0x444, 0x446, 0x447, 0x448, 0x449, 0x44A, 0x44B, 0x44C, 0x44D, 0x44E, 0x44F, 0x454, 0x490, 0x491
    ).orElse {
      case x: Int => x.asInstanceOf[Int]
    }

  }

  object GetString {
    val mapper = (x: (Int, ByteString)) => CubeString.mapping(x._1).toChar

    def unapply(bytes: ByteString): Option[(String, ByteString)] = bytes match {
      case ByteString.empty =>
        None
      case something =>
        val (forStr, rest) = GetUchars.uchars(bytes).span(i => i._1 > 0)
        Option((forStr.map(mapper).mkString, ByteString(rest.take(1).flatMap(_._2).toArray)))
    }
  }

  val >>##:: = GetString

  val >>: = GetInt

  object GetIp {
    def unapply(List: ByteString): Option[(String, ByteString)] = List match {
      case GetUChar(a) ##:: GetUChar(b) ##:: GetUChar(c) ##:: rest =>
        Option(s"$a.$b.$c.x", rest)
      case _ =>
        None
    }
  }
  val >~: = GetIp

  sealed trait ParsedResponse

  case class PlayerCns(cns: List[Int]) extends ParsedResponse
  object GetPlayerCns{
    def unapply(list: ByteString): Option[PlayerCns] = list match {
      case extAck >>: extVersion >>: 0 >>: -10 >>: rest =>
        def go(stuff: ByteString, l: List[Int]): List[Int] = {
          stuff match {
            case cn >>: other =>
              go(other, l :+ cn)
            case _ => l
          }
        }
        Option(PlayerCns(go(rest, List.empty)))
      case _ => None
    }
  }


  case class ServerInfoReply(protocol: Int, mode: Int, numPlayers: Int, minRemain: Int, mapName: String, desc: String, maxClients: Int, ping: Int) extends ParsedResponse

  object GetServerInfoReply {
    def unapply(List: ByteString): Option[ServerInfoReply] = List match {
      case protocol >>: mode >>: numPlayers >>: minRemain >>: mapName >>##:: desc >>##:: maxClients >>: ping >>: ByteString.empty =>
        Option(ServerInfoReply(protocol, mode, numPlayers, minRemain, mapName, desc, maxClients, ping))
      case _ => None
    }
  }

  case class PlayerInfoReply(clientNum: Int, ping: Int, name: String, team: String, frags: Int, flagScore: Int, deaths: Int,
                              teamkills: Int, accuracy: Int, health: Int, armour: Int, weapon: Int, role: Int, state: Int, ip: String) extends ParsedResponse
  object GetPlayerInfos {
    def unapply(list: ByteString): Option[PlayerInfoReply] = list match {
      case extAck >>: extVersion >>: 0 >>: -11 >>: clientNum >>: ping >>: name >>##:: team >>##::
      frags >>: flagscore >>: deaths >>: teamkills >>: accuracy >>: health >>: armour >>: gunSelected >>: role >>: state >>: ip >~: ByteString.empty =>
        Option(PlayerInfoReply(
        clientNum, ping, name, team, frags, flagscore, deaths, teamkills, accuracy, health, armour, gunSelected, role, state, ip
        ))
      case _ => None
    }
  }
  case class TeamScore(name: String, frags: Int, flags: Int)
  object GetTeamScore {
    def unapply(list: ByteString): Option[(TeamScore, ByteString)] = {
      list match {
        case name >>##:: frags >>: flags >>: -1 >>: other =>
          Option((TeamScore(name, frags, flags), other))
        case _ => None
      }
    }
  }
  case class TeamInfos(gameMode: Int, minRemain: Int, teams: List[TeamScore]) extends ParsedResponse
  object GetTeamInfos {
    def unapply(list: ByteString): Option[TeamInfos] = list match {
      case extAck >>: extVersion >>: 0 >>: gamemode >>: minremain >>: rest =>
        def go(items: ByteString): List[TeamScore] = {
          items match {
            case GetTeamScore(teamScore, other) =>
              teamScore +: go(other)
            case _ => List.empty
          }
        }
        Option(TeamInfos(gamemode, minremain, go(rest)))
      case _ => None
    }
  }
}