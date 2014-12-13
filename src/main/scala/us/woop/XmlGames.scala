package us.woop

import us.woop.CaptureGames.Parser.{FragGameBuilder, FlagGameBuilder, FoundGame}

object XmlGames {

  def foundGameXml(foundGame: FoundGame): scala.xml.Elem = {
    <game map={foundGame.header.map} mode={foundGame.header.mode.name} state={foundGame.header.state} winner={
    (foundGame.game match {
      case Left(FlagGameBuilder(_, _, List(a, b))) if a.flags > b.flags => Option(a.name)
      case Left(FlagGameBuilder(_, _, List(a, b))) if a.flags < b.flags => Option(b.name)
      case Right(FragGameBuilder(_, _, List(a, b))) if a.frags > b.frags => Option(a.teamName)
      case Right(FragGameBuilder(_, _, List(a, b))) if a.frags < b.frags => Option(b.teamName)
      case _ => None
    }).orNull
    }>
    {foundGame.game match {
      case Left(FlagGameBuilder(_, scores, teamScores)) =>
        for { team <- teamScores.sortBy(_.flags).reverse }
        yield <team name={team.name} flags={team.flags.toString} frags={team.frags.toString}>
          { for { player <- scores.filter(_.team == team.name).sortBy(_.flag).reverse }
            yield <player flags={player.flag.toString} frags={player.frag.toString} name={player.name} host={player.host}/>
            } </team>
      case Right(FragGameBuilder(_, scores, teamScores)) =>
        for { team <- teamScores.sortBy(_.frags).reverse }
        yield <team name={team.teamName} frags={team.frags.toString}>
            { for { player <- scores.filter(_.team == team.teamName).sortBy(_.frag).reverse }
            yield <player frags={player.frag.toString} name={player.name} host={player.host}/>
            } </team>
    }}
    </game>
  }

}
