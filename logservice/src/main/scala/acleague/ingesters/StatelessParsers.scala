package acleague.ingesters

case class DemoRecorded(dateTime: String, mode: String, map: String, size: String)
object DemoRecorded {
  val capture = """Demo "(.*):\s+(.*), maps\/([^\s]+), ([^\s]+)" recorded\.""".r
  def unapply(input: String): Option[DemoRecorded] = {
    input match {
      case capture(dateTime, mode, map, size) =>
        Option(DemoRecorded(dateTime, mode, map, size))
      case _ =>
        None
    }
  }
}

case class DemoWritten(filename: String, size: String)
object DemoWritten {
  val capture = """demo written to file "([^"]+)" \(([^\)]+)\)""".r
  def unapply(input: String): Option[DemoWritten] = {
    input match {
      case capture(filename, size) => Some(DemoWritten(filename, size))
      case _ => None
    }
  }
}

case class GameFinishedHeader(mode: GameMode.GameMode, map: String, state: String)
object GameFinishedHeader {
  val capture = """Game status:\s+(.*)\s+on\s+([^\s]+), game finished, ([^\s]+)""".r
  def unapply(input: String): Option[GameFinishedHeader] = {
    input match {
      case capture(mode, map, state) =>
        GameMode.gamemodes.find(_.name == mode).map(foundMode => GameFinishedHeader(foundMode, map, state))
      case _ => None
    }
  }
}
object VerifyTableHeader {
  def unapply(input: String): Boolean = {
    val capture = """cn\s+name\s+.*""".r
    input match {
      case capture() => true
      case _ => false
    }
  }
}
object TeamModes {

  object FragStyle {

    case class IndividualScore(cn: Int, name: String, team: String, frag: Int, death: Int, tk: Int, ping: Int, role: String, host: String)

    object IndividualScore {
      def unapply(input: String): Option[IndividualScore] = {
        val capture = """\s?(\d+)\s([^\s]+)\s+([^\s]+)\s+(-?\d+)\s+(\d+)\s+(\d+)\s+(-?\d+)\s+([^\s]+)\s+([^\s]+)\s*""".r
        input match {
          case capture(cn, name, team, frag, death, tk, ping, role, host) =>
            Option(IndividualScore(cn.toInt, name, team, frag.toInt, death.toInt, tk.toInt, ping.toInt, role, host))
        }
      }
    }

    case class TeamScore(teamName: String, players: Int, frags: Int)

    object TeamScore {
      def unapply(input: String): Option[TeamScore] = {
        val capture = """Team\s+([^\s]+):\s+(\d+)\s+players,\s+(-?\d+)\s+frags.*""".r
        input match {
          case capture(teamName, players, frags) =>
            Option(TeamScore(teamName, players.toInt, frags.toInt))
        }
      }
    }

  }

  object FlagStyle {

    case class IndividualScore(cn: Int, name: String, team: String, flag: Int, frag: Int, death: Int, tk: Int, ping: Int, role: String, host: String)

    object IndividualScore {
      def unapply(input: String): Option[IndividualScore] = {
        val capture = """\s?(\d+)\s([^\s]+)\s+([^\s]+)\s+(\d+)\s+(-?\d+)\s+(\d+)\s+(\d+)\s+(-?\d+)\s+([^\s]+)\s+([^\s]+)\s*""".r
        input match {
          case capture(cn, name, team, flag, frag, death, tk, ping, role, host) =>
            Option(IndividualScore(cn.toInt, name, team, flag.toInt, frag.toInt, death.toInt, tk.toInt, ping.toInt, role, host))
          case _ => None
        }
      }
    }

    case class TeamScore(name: String, players: Int, frags: Int, flags: Int)

    object TeamScore {
      def unapply(input: String): Option[TeamScore] = {
        val capture = """Team\s+([^\s]+):\s+(\d+)\s+players,\s+(-?\d+)\s+frags,\s+(\d+)\s+flags""".r
        input match {
          case capture(name, players, frags, flags) =>
            Option(TeamScore(name, players.toInt, frags.toInt, flags.toInt))
          case _ => None
        }
      }
    }

  }

}
