package acleague.ingesters

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
        TeamModes.FlagStyle.IndividualScore(1,"Drakas", "RVSF", 5,1,2,3,4,"admin", "12.2.2.2"),
        TeamModes.FlagStyle.IndividualScore(2,"Fragg", "CLA", 15,11,12,13,14,"normal", "12.2.2.5")
      ),
      teamScores = List(
        TeamModes.FlagStyle.TeamScore("RVSF", 1, 1, 5),
        TeamModes.FlagStyle.TeamScore("CLA", 1, 15, 11)
      ))
    )
    )
  }
}
