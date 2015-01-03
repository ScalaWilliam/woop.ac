package acleague.ingesters

sealed trait GameDuration {
  def next(input: String): GameDuration
}
case object NoDurationFound extends GameDuration {
  override def next(input: String): GameDuration = {
    input match {
      case GameInProgressHeader(GameInProgressHeader(mode, remaining, map, state)) =>
        GameInProgress(remaining, remaining)
      case _ => NoDurationFound
    }
  }
}
case class GameInProgress(duration: Int, remain: Int) extends GameDuration {
  override def next(input: String): GameDuration = {
    input match {
      case GameFinishedHeader(GameFinishedHeader(_, _, _)) =>
        GameFinished(duration)
      case GameInProgressHeader(GameInProgressHeader(_, remaining, _, _)) =>
        if ( remaining > remain || remaining > duration ) {
          // new map - though we should never get here, really
          GameInProgress(remaining, remaining)
        } else {
          GameInProgress(duration, remaining)
        }
      case _ => this
    }
  }
}
case class GameFinished(duration: Int) extends GameDuration {
  override def next(input: String): GameDuration = input match {
    case GameInProgressHeader(GameInProgressHeader(mode, remaining, map, state)) =>
      GameInProgress(remaining, remaining)
    case _ => this
  }
}

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
                ReadingFlagScores(FlagGameBuilder(header, List.empty, List.empty, List.empty))
              case VerifyTableHeader() if mode.isFrag =>
                ReadingFragScores(FragGameBuilder(header, List.empty, List.empty, List.empty))
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
case class FragGameBuilder(header: GameFinishedHeader, scores: List[TeamModes.FragStyle.IndividualScore], disconnectedScores: List[TeamModes.FragStyle.IndividualScoreDisconnected], teamScores: List[TeamModes.FragStyle.TeamScore])
case class FlagGameBuilder(header: GameFinishedHeader, scores: List[TeamModes.FlagStyle.IndividualScore], disconnectedScores: List[TeamModes.FlagStyle.IndividualScoreDisconnected], teamScores: List[TeamModes.FlagStyle.TeamScore])
case class ReadingFragScores(builder: FragGameBuilder) extends ParserState {
  def next(input: String): ParserState = {
    input match {
      case text if TeamModes.FragStyle.IndividualScore.unapply(text).isDefined =>
        val newScore = TeamModes.FragStyle.IndividualScore.unapply(text).get
        if ( (FoundGame.teamsMap contains newScore.team) && (newScore.score != 0)) {
          ReadingFragScores(builder.copy(scores = builder.scores :+ newScore.copy(team = FoundGame.teamsMap(newScore.team))))
        } else this
      case text if TeamModes.FragStyle.TeamScore.unapply(text).isDefined =>
        val newScore = TeamModes.FragStyle.TeamScore.unapply(text).get
        if ( FoundGame.teams.contains(newScore.teamName) ) {
          ReadingFragScores(builder.copy(teamScores = builder.teamScores :+ newScore))
        } else this
      case text if TeamModes.FragStyle.IndividualScoreDisconnected.unapply(text).isDefined =>
        val newScore = TeamModes.FragStyle.IndividualScoreDisconnected.unapply(text).get
        if ( FoundGame.teamsMap.contains(newScore.team) && (newScore.frag != 0)) {
          ReadingFragScores(builder.copy(disconnectedScores = builder.disconnectedScores :+ newScore.copy(team = FoundGame.teamsMap(newScore.team))))
        } else this
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
        val newScore = TeamModes.FlagStyle.IndividualScore.unapply(text).get
        if ( FoundGame.teamsMap.contains(newScore.team) && (newScore.score != 0) ) {
          ReadingFlagScores(builder.copy(scores = builder.scores :+ newScore.copy(team = FoundGame.teamsMap(newScore.team))))
        } else this
      case text if TeamModes.FlagStyle.IndividualScoreDisconnected.unapply(text).isDefined =>
        val newScore = TeamModes.FlagStyle.IndividualScoreDisconnected.unapply(text).get
        if ( FoundGame.teamsMap.contains(newScore.team) && (newScore.frag != 0) ) {
          ReadingFlagScores(builder.copy(disconnectedScores = builder.disconnectedScores :+ newScore.copy(team = FoundGame.teamsMap(newScore.team))))
        } else this
      case text if TeamModes.FlagStyle.TeamScore.unapply(text).isDefined =>
        val newScore = TeamModes.FlagStyle.TeamScore.unapply(text).get
        if ( FoundGame.teams.contains(newScore.name)) {
          ReadingFlagScores(builder.copy(teamScores = builder.teamScores :+ newScore))
        } else this
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
  val teamsMap = Map("RVSF" -> "RVSF", "CLA" -> "CLA", "RSPC"-> "RVSF", "CSPC" -> "CLA")
  val teams = Seq("RVSF", "CLA")
  def apply(builder: FlagGameBuilder): FoundGame = {
    FoundGame(builder.header, Left(builder))
  }
  def apply(builder: FragGameBuilder): FoundGame = {
    FoundGame(builder.header, Right(builder))
  }
  def example = {
    val gameFinishedHeader = GameFinishedHeader(
      mode = GameMode.TKTF,
      map = "ac_test",
      state = "open"
    )
    FoundGame(
      header = gameFinishedHeader,
      game = Left(FlagGameBuilder(
      header = gameFinishedHeader,
      scores = List(
        TeamModes.FlagStyle.IndividualScore(1,"Drakas", "RVSF",1, 5,1,2,3,4,"admin", "12.2.2.2"),
        TeamModes.FlagStyle.IndividualScore(2,"Fragg", "CLA", 2,15,11,12,13,14,"normal", "12.2.2.5")
      ),
      disconnectedScores = List.empty,
      teamScores = List(
        TeamModes.FlagStyle.TeamScore("RVSF", 1, 10, 5),
        TeamModes.FlagStyle.TeamScore("CLA", 1, 15, 11)
      ))
    )
    )
  }
}