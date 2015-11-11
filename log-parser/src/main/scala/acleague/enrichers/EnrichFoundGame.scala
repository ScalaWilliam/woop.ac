package acleague.enrichers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.{Date}
import acleague.ingesters.{FlagGameBuilder, FoundGame, FragGameBuilder}
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.{Json, JsObject}
import scala.util.hashing.MurmurHash3
import scala.xml.UnprefixedAttribute

case class GameJsonFound(jsonGame: JsonGame)

object JsonGame {
  implicit val Af = Json.format[JsonGamePlayer]
  implicit val Bf = Json.format[JsonGameTeam]
  implicit val fmt = Json.format[JsonGame]

  def build(foundGame: FoundGame, date: ZonedDateTime, serverId: String, duration: Int): JsonGame = {
    val fdt = date.format(DateTimeFormatter.ISO_INSTANT)

    JsonGame(
      id = fdt, gameTime = date, server = serverId, duration = duration,
      map = foundGame.header.map,
      mode = foundGame.header.mode.name,
      state = foundGame.header.state,
      teams = {
        val tt = foundGame.game.fold(_.teamScores.map(_.project), _.teamScores.map(_.project))
        val tp = foundGame.game.fold(g => (g.scores ++ g.disconnectedScores).map(_.project),
          g => (g.scores ++ g.disconnectedScores).map(_.project))

        for {team <- tt.sortBy(team => (team.flags, team.frags)).reverse.toList}
          yield JsonGameTeam(
            name = team.name, flags = team.flags, frags = team.frags,
            players = {
              for {player <- tp.filter(_.team == team.name).sortBy(p => (p.flag, p.frag)).reverse}
                yield JsonGamePlayer(
                  name = player.name, host = player.host, score = player.score, flags = player.flag,
                  frags = player.frag, deaths = player.death
                )
            }
          )
      }
    )
  }
}

object EnrichFoundGame {

  case class GameXmlReady(xml: String)


  def apply(foundGame: FoundGame)(date: DateTime, serverId: String, duration: Int): GameXmlReady = {
    val gameXml = foundGameXml(foundGame)
    val gameId = Math.abs(MurmurHash3.productHash(serverId, date, 1337))
    val utcDate = {
      val utcDatetime = new DateTime(date).withZone(DateTimeZone.forID("UTC"))
      ISODateTimeFormat.dateTimeNoMillis().print(utcDatetime)
    }

    val newGameXml = gameXml.copy(attributes =
      new UnprefixedAttribute("id", s"$gameId",
        new UnprefixedAttribute("duration", s"$duration",
          new UnprefixedAttribute("date", utcDate,
            new UnprefixedAttribute("server", serverId,
              gameXml.attributes)
          )
        )
      )
    )
    GameXmlReady(s"$newGameXml")
  }

  def foundGameXml(foundGame: FoundGame): scala.xml.Elem = {


    <game map={foundGame.header.map} mode={foundGame.header.mode.name} state={foundGame.header.state} winner={(foundGame.game match {
      case Left(FlagGameBuilder(_, _, _, List(a, b))) if a.flags > b.flags => Option(a.name)
      case Left(FlagGameBuilder(_, _, _, List(a, b))) if a.flags < b.flags => Option(b.name)
      case Right(FragGameBuilder(_, _, _, List(a, b))) if a.frags > b.frags => Option(a.teamName)
      case Right(FragGameBuilder(_, _, _, List(a, b))) if a.frags < b.frags => Option(b.teamName)
      case _ => None
    }).orNull}>
      {val (teams, players) = foundGame.game match {
      case Left(FlagGameBuilder(_, scores, disconnectedScores, teamScores)) =>
        teamScores.map(_.project) -> (scores ++ disconnectedScores).map(_.project)
      case Right(FragGameBuilder(_, scores, disconnectedScores, teamScores)) =>
        teamScores.map(_.project) -> (scores ++ disconnectedScores).map(_.project)
    }
    for {team <- teams.sortBy(team => (team.flags, team.frags)).reverse}
      yield <team name={team.name} flags={team.flags.map(_.toString).orNull} frags={team.frags.toString}>
        {for {player <- players.filter(_.team == team.name).sortBy(p => (p.flag, p.frag)).reverse}
          yield <player name={player.name} host={player.host.orNull}
                        score={player.score.map(_.toString).orNull} flags={player.flag.map(_.toString).orNull}
                        frags={player.frag.toString} deaths={player.death.toString}/>}
      </team>}
    </game>
  }

}

case class JsonGamePlayer(name: String, host: Option[String], score: Option[Int],
                          flags: Option[Int], frags: Int, deaths: Int)

case class JsonGameTeam(name: String, flags: Option[Int], frags: Int, players: List[JsonGamePlayer])

case class JsonGame(id: String, gameTime: ZonedDateTime, map: String, mode: String, state: String,
                    teams: List[JsonGameTeam], server: String, duration: Int) {
  def toJson: JsObject = {
    Json.toJson(this)(JsonGame.fmt).asInstanceOf[JsObject]
  }

  import org.scalactic._
  def validate: JsonGame Or ErrorMessage = {
    def numberOfPlayers = teams.map(_.players.size).sum
    def averageFrags = teams.flatMap(_.players.map(_.frags)).sum / numberOfPlayers
    if ( duration < 10 ) Bad(s"Duration is $duration, expecting at least 10")
    else if ( numberOfPlayers < 4) Bad(s"Player count is $numberOfPlayers, expecting 4 or more.")
    else if ( teams.size < 2 ) Bad(s"Expected team size >= 2, got ${teams.size}")
    else if ( averageFrags < 15 ) Bad(s"Average frags $averageFrags, expected >= 15 ")
    else Good(this)
  }
}