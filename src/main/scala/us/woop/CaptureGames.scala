package us.woop

object CaptureGames {

  object Parser {
    sealed trait ParserState {
      def next(input: String): ParserState
    }
    case object NothingFound extends ParserState {
      def next(input: String): ParserState = {
        input match {
          case GameFinishedHeader(header @ GameFinishedHeader(mode, _, _)) =>
            new ParserState {
              override def next(input: String): ParserState =
              input match {
                case VerifyTableHeader() if mode.isFlag =>
                    ReadingFlagScores(FlagGameBuilder(header, List.empty, List.empty))
                case VerifyTableHeader() if mode.isFrag =>
                  ReadingFragScores(FragGameBuilder(header, List.empty, List.empty))
                case _ => NothingFound
              }
            }
          case _ => NothingFound
        }
      }
    }
    case object NotEnoughPlayersFailure extends ParserState {
      def next(input: String) = NothingFound.next(input)
    }
    case object NotEnoughTeamsFailure extends ParserState {
      def next(input: String) = NothingFound.next(input)
    }
    case class UnexpectedInput(line: String) extends ParserState {
      def next(input: String) = NothingFound.next(input)
    }
    case class FragGameBuilder(header: GameFinishedHeader, scores: List[TeamModes.FragStyle.IndividualScore], teamScores: List[TeamModes.FragStyle.TeamScore]) 
    case class FlagGameBuilder(header: GameFinishedHeader, scores: List[TeamModes.FlagStyle.IndividualScore], teamScores: List[TeamModes.FlagStyle.TeamScore])
    case class ReadingFragScores(builder: FragGameBuilder) extends ParserState {
      def next(input: String): ParserState = {
        input match {
          case text if TeamModes.FragStyle.IndividualScore.unapply(text).isDefined =>
            ReadingFragScores(builder.copy(scores = builder.scores :+ TeamModes.FragStyle.IndividualScore.unapply(text).get))
          case text if TeamModes.FragStyle.TeamScore.unapply(text).isDefined =>
            ReadingFragScores(builder.copy(teamScores = builder.teamScores :+ TeamModes.FragStyle.TeamScore.unapply(text).get))
          case "" if builder.scores.isEmpty =>
            NotEnoughPlayersFailure
          case "" if builder.teamScores.size != 2 =>
            NotEnoughTeamsFailure
          case "" => FoundGame(builder)
          case _ => UnexpectedInput(input)
        }
      }
    }
    case class ReadingFlagScores(builder: FlagGameBuilder) extends ParserState {
      def next(input: String): ParserState = {
        input match {
          case text if TeamModes.FlagStyle.IndividualScore.unapply(text).isDefined =>
            ReadingFlagScores(builder.copy(scores = builder.scores :+ TeamModes.FlagStyle.IndividualScore.unapply(text).get))
          case text if TeamModes.FlagStyle.TeamScore.unapply(text).isDefined =>
            ReadingFlagScores(builder.copy(teamScores = builder.teamScores :+ TeamModes.FlagStyle.TeamScore.unapply(text).get))
          case "" if builder.scores.isEmpty =>
            NotEnoughPlayersFailure
          case "" if builder.teamScores.size != 2 =>
            NotEnoughTeamsFailure
          case "" => FoundGame(builder)
          case _ => UnexpectedInput(input)
        }
      }
    }
    case class FoundGame(header: GameFinishedHeader, game: Either[FlagGameBuilder, FragGameBuilder]) extends ParserState {
      def next(input: String) = NothingFound.next(input)
    }
    object FoundGame {
      def apply(builder: FlagGameBuilder): FoundGame = {
        FoundGame(builder.header, Left(builder))
      }
      def apply(builder: FragGameBuilder): FoundGame = {
        FoundGame(builder.header, Right(builder))
      }
    }
  }
  object GameMode {
    sealed trait Style {
      def isFrag: Boolean
      def isFlag: Boolean
    }
    sealed trait FragStyle extends Style {
      def isFrag = true
      def isFlag = false
    }
    sealed trait FlagStyle extends Style {
      def isFrag = false
      def isFlag = true
    }
    sealed abstract class GameMode(val name: String) extends Style
    case object HTF extends GameMode("hunt the flag") with FlagStyle
    case object CTF extends GameMode("ctf") with FlagStyle
    case object TOSOK extends GameMode("team one shot, one kill") with FragStyle
    case object TDM extends GameMode("team deathmatch") with FragStyle
    case object TS extends GameMode("team survivor") with FragStyle
    case object TKTF extends GameMode("team keep the flag") with FlagStyle
    val gamemodes: Seq[GameMode] = Seq(HTF, CTF, TOSOK, TDM, TKTF)
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
}
