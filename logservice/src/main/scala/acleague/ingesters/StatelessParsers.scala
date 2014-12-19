package acleague.ingesters

case class DemoRecorded(dateTime: String, mode: String, map: String, size: String)
object DemoRecorded {
  val capture = """Demo "(.*):\s+(.*), ([^\s]+), (\d+[^\s]+), .*" recorded\.""".r
  val capture2 = """Demo "(.*):\s+(.*), ([^\s]+), (\d+[^\s]+)" recorded\.""".r
  def unapply(input: String): Option[DemoRecorded] = input match {
    case capture(dateTime, mode, map, size) => Option(DemoRecorded(dateTime, mode, map, size))
    case capture2(dateTime, mode, map, size) => Option(DemoRecorded(dateTime, mode, map, size))
    case _ => None
  }
}

case class DemoWritten(filename: String, size: String)
object DemoWritten {
  val capture = """demo written to file "([^"]+)" \(([^\)]+)\)""".r
  def unapply(input: String): Option[DemoWritten] =
    for { capture(filename, size) <- Option(input) }
    yield DemoWritten(filename, size)
}

case class GameFinishedHeader(mode: GameMode.GameMode, map: String, state: String)
object GameFinishedHeader {
  val capture = """Game status:\s+(.*)\s+on\s+([^\s]+), game finished, ([^\s]+), \d+ clients""".r
  def unapply(input: String): Option[GameFinishedHeader] =
    for {
      capture(mode, map, state) <- Option(input)
      foundMode <- GameMode.gamemodes.find(_.name == mode)
    } yield GameFinishedHeader(foundMode, map, state)
}
case class GameInProgressHeader(mode: GameMode.GameMode, remaining: Int, map: String, state: String)
object GameInProgressHeader {
  val capture = """Game status:\s+(.*)\s+on\s+([^\s]+), (\d+) minutes remaining, ([^\s]+), \d+ clients""".r
  def unapply(input: String): Option[GameInProgressHeader] =
    for {
      capture(mode, map, remain, state) <- Option(input)
      foundMode <- GameMode.gamemodes.find(_.name == mode)
    } yield GameInProgressHeader(foundMode, remain.toInt, map, state)
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

    case class IndividualScore(cn: Int, name: String, team: String, score: Int, frag: Int, death: Int, tk: Int, ping: Int, role: String, host: String) extends CreatesGenericIndividualScore {
      override def project: GenericIndividualScore =
        GenericIndividualScore(name, team, None, Option(score), frag, death, Option(host))
    }

    object IndividualScore {
      val capture = """\s?(\d+)\s([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+(-?\d+)\s+(\d+)\s+(\d+)\s+(-?\d+)\s+([^\s]+)\s+([^\s]+)\s*""".r
      def unapply(input: String): Option[IndividualScore] =
        for { capture(cn, name, team, score, frag, death, tk, ping, role, host) <- Option(input) }
        yield IndividualScore(cn.toInt, name, team, score.toInt, frag.toInt, death.toInt, tk.toInt, ping.toInt, role, host)
    }

    case class IndividualScoreDisconnected(name: String, team: String, frag: Int, death: Int) extends CreatesGenericIndividualScore {
      override def project: GenericIndividualScore = GenericIndividualScore(
        name, team, None, None, frag, death, None
      )
    }

    object IndividualScoreDisconnected {
      def unapply(input: String): Option[IndividualScoreDisconnected] = {
        val capture = """\s+([^\s]+)\s+([^\s]+)\s+(-?\d+)\s+(-?\d+)\s+\-\s+\-\s+disconnected""".r
        for { capture(name, team, frag, death) <- Option(input) }
        yield IndividualScoreDisconnected(name, team, frag.toInt, death.toInt)
      }
    }
    case class TeamScore(teamName: String, players: Int, frags: Int)  extends CreatesGenericTeamScore {
      override def project: GenericTeamScore = GenericTeamScore(teamName, players, None, frags)
    }

    object TeamScore {
      val capture = """Team\s+([^\s]+):\s+(\d+)\s+players,\s+(-?\d+)\s+frags.*""".r
      def unapply(input: String): Option[TeamScore] =
        for { capture(teamName, players, frags) <- Option(input) }
        yield TeamScore(teamName, players.toInt, frags.toInt)
    }

  }

  case class GenericTeamScore(name: String, players: Int, flags: Option[Int], frags: Int)

  trait CreatesGenericTeamScore {
    def project: GenericTeamScore
  }

  case class GenericIndividualScore(name: String, team: String, flag: Option[Int], score: Option[Int], frag: Int, death:Int, host: Option[String])
  trait CreatesGenericIndividualScore {
    def project: GenericIndividualScore
  }
  object FlagStyle {
    case class IndividualScore(cn: Int, name: String, team: String, flag: Int, score: Int, frag: Int, death: Int, tk: Int, ping: Int, role: String, host: String) extends CreatesGenericIndividualScore {
      def project = GenericIndividualScore(name, team, Option(flag), Option(score), frag, death, Option(host))
    }

    object IndividualScore {
      val capture = """\s?(\d+)\s([^\s]+)\s+([^\s]+)\s+(\d+)\s+(-?\d+)\s+(-?\d+)\s+(\d+)\s+(\d+)\s+(-?\d+)\s+([^\s]+)\s+([^\s]+)\s*""".r
      def unapply(input: String): Option[IndividualScore] = {
        for { capture(cn, name, team, flag, score, frag, death, tk, ping, role, host) <- Option(input) }
        yield IndividualScore(cn.toInt, name, team, flag.toInt, score.toInt, frag.toInt, death.toInt, tk.toInt, ping.toInt, role, host)
      }
    }

    case class IndividualScoreDisconnected(name: String, team: String, flag: Int, frag: Int, death: Int)  extends CreatesGenericIndividualScore {
      override def project: GenericIndividualScore = GenericIndividualScore(
        name, team, Option(flag), None, frag, death, None)
    }

    object IndividualScoreDisconnected {
      val capture = """\s+([^\s]+)\s+([^\s]+)\s+(\d+)\s+(-?\d+)\s+(-?\d+)\s+\-\s+\-\s+disconnected""".r
      def unapply(input: String): Option[IndividualScoreDisconnected] = {
        for { capture(name, team, flag, frag, death) <- Option(input) }
        yield IndividualScoreDisconnected(name, team, flag.toInt, frag.toInt, death.toInt)
      }
    }

    case class TeamScore(name: String, players: Int, frags: Int, flags: Int) extends CreatesGenericTeamScore {
      override def project: GenericTeamScore = GenericTeamScore(name, players, Option(flags), frags)
    }

    object TeamScore {
      val capture = """Team\s+([^\s]+):\s+(\d+)\s+players,\s+(-?\d+)\s+frags,\s+(\d+)\s+flags""".r
      def unapply(input: String): Option[TeamScore] =
        for { capture(name, players, frags, flags) <- Option(input) }
        yield TeamScore(name, players.toInt, frags.toInt, flags.toInt)
    }

  }

}
