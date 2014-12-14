package acleague.enrichers

import java.util.{Date}
import acleague.ingesters.{FlagGameBuilder, FoundGame, FragGameBuilder}
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.ISODateTimeFormat
import scala.util.hashing.MurmurHash3
import scala.xml.UnprefixedAttribute

object EnrichFoundGame {

  case class GameXmlReady(xml: String)

  def apply(foundGame: FoundGame)(date: Date, serverId: String): GameXmlReady = {
    val gameXml = foundGameXml(foundGame)
    val gameId = Math.abs(MurmurHash3.productHash(serverId, date, 1337))
    val utcDate = {
      val utcDatetime = new DateTime(date).withZone(DateTimeZone.forID("UTC"))
      ISODateTimeFormat.dateTimeNoMillis().print(utcDatetime)
    }

    val newGameXml = gameXml.copy(attributes =
      new UnprefixedAttribute("id", s"$gameId",
        new UnprefixedAttribute("date", utcDate,
          new UnprefixedAttribute("server", serverId,
            gameXml.attributes)
        )
      )
    )
    GameXmlReady(s"$newGameXml")
  }

  def foundGameXml(foundGame: FoundGame): scala.xml.Elem = {


    <game map={foundGame.header.map} mode={foundGame.header.mode.name} state={foundGame.header.state} winner={
    (foundGame.game match {
      case Left(FlagGameBuilder(_, _, _, List(a, b))) if a.flags > b.flags => Option(a.name)
      case Left(FlagGameBuilder(_, _, _, List(a, b))) if a.flags < b.flags => Option(b.name)
      case Right(FragGameBuilder(_, _, _, List(a, b))) if a.frags > b.frags => Option(a.teamName)
      case Right(FragGameBuilder(_, _, _, List(a, b))) if a.frags < b.frags => Option(b.teamName)
      case _ => None
    }).orNull
    }>
    {
      val (teams, players) = foundGame.game match {
        case Left(FlagGameBuilder(_, scores, disconnectedScores, teamScores)) =>
          teamScores.map(_.project) -> (scores ++ disconnectedScores).map(_.project)
        case Right(FragGameBuilder(_, scores, disconnectedScores, teamScores)) =>
          teamScores.map(_.project) -> (scores ++ disconnectedScores).map(_.project)
      }
      for { team <- teams.sortBy(team => (team.flags, team.frags)).reverse }
        yield <team name={team.name} flags={team.flags.map(_.toString).orNull} frags={team.frags.toString}>
        {
        for { player <- players.filter(_.team == team.name).sortBy(p => (p.flag, p.frag)) }
          yield <player name={player.name} host={player.host.orNull} score={player.score.toString} flags={player.flag.map(_.toString).orNull} frags={player.frag.toString}/>
        }
      </team>
    }
    </game>
  }

}
