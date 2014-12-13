package controllers

import org.basex.server.ClientSession
import play.api.mvc._
import play.twirl.api.Html

object Main extends Controller {
  def using[T <: { def close(): Unit }, V](a: => T)(f: T=> V):V  = {
    val i = a
    try f(i)
    finally i.close()
  }
  def readx = Action{
    request =>
      val conn = new ClientSession("odin", 1236, "admin", "admin")
      conn.execute("open acleague")
      val result = using(conn.query(
        """
          |let $earliest := string(current-dateTime() - xs:dayTimeDuration("P7D"))
          |for $game in /game
          |let $date := data($game/@date)
          |where $date ge $earliest
          |order by $date descending
          |return $game
        """.stripMargin)) {
        _.execute()
      }

      Ok(result)
  }
  def read = Action{
    request =>
      val conn = new ClientSession("odin", 1236, "admin", "admin")
      conn.execute("open acleague")
      val result = using(conn.query(
        """
          |let $earliest := string(current-dateTime() - xs:dayTimeDuration("P7D"))
          |for $game in /game
          |let $date := data($game/@date)
          |let $has-flags := not(empty($game//@flags))
          |where $date ge $earliest
          |order by $date descending
          |return
          |<article class="game">
          |<header><h2>{data($game/@mode)} @ {data($game/@map)} on {data($date)}</h2></header>
          |<div class="teams">
          |{
          |for $team in $game/team[@name]
          |let $name := data($team/@name)
          |let $low-name := lower-case($name)
          |return
          |<div class="{$low-name || " team"}">
          |
          |<h3>{$name}</h3>
          |
          |<span class="score">{if ( $has-flags ) then (data($team/@flags)) else (data($team/@frags))}</span>
          |{if ( $has-flags ) then (<span class="subscore">{data($team/@frags)}</span>) else ()}
          |<table class="players">
          |<tbody>
          |{ for $player in $team/player
          |return <tr>
          |<th class="score">{if ( $has-flags ) then (data($player/@flags)) else (data($player/@frags))}</th>
          |{if ( $has-flags ) then (<th class="subscore">{data($player/@frags)}</th>) else ()}
          |<td class="name">{data($player/@name)}</td>
          |</tr>
          |}
          |</tbody>
          |</table>
          |</div>
          |}
          |</div>
          |</article>
          |
        """.stripMargin)) {
        _.execute()
      }

      Ok(views.html.main(Html(result)))
  }
}