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
  def servers = Action {
    request =>
              Ok(views.html.servers())
  }
  def questions = Action {
    request =>
              Ok(views.html.questions())
  }
  def withSession[T](x: ClientSession => T): T = {
    using(new ClientSession("odin", 1236, "admin", "admin")) { c =>
      c.execute("open acleague")
      x(c)
    }
  }
  def read = Action{
    request =>
      withSession { conn =>
//        val header = using(conn.query("/article[@id='top']"))(_.execute())
        val result = using(conn.query(
          """
          |let $earliest := (adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P7D"), ())) cast as xs:date
          |for $game in /game
          |let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
          |let $date := xs:date($dateTime cast as xs:date)
          |let $day-ago := adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P1D"), ()) cast as xs:date
          |let $date-text :=
          | if ( $date = xs:date(current-date()) ) then (" today")
          | else if ( $date = $day-ago ) then (" yesterday")
          | else (" on "|| $date)
          |let $has-flags := not(empty($game//@flags))
          |where $date ge $earliest
          |where count($game//player) ge 4
          |order by data($game/@date) descending
          |
          |return
          |<article class="game" style="{"background-image:url('/assets/maps/"||data($game/@map)||".jpg')"}"><div class="w">
          |<header><h2>{data($game/@mode)} @ {data($game/@map)} {$date-text}</h2></header>
          |<div class="teams">
          |{
          |for $team in $game/team[@name]
          |let $name := data($team/@name)
          |let $low-name := lower-case($name)
          |return
          |<div class="{$low-name || " team"}">
          |<div class="team-header">
          |<h3><img src="{"/assets/"||$low-name||".png"}"/></h3>
          |<div class="result">
          |
          |<span class="score">{if ( $has-flags ) then (data($team/@flags)) else (data($team/@frags))}</span>
          |{if ( $has-flags ) then (<span class="subscore">{data($team/@frags)}</span>) else ()}
          |</div>
          |</div>
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
          |</div></div>
          |</article>
          |
        """.stripMargin)) {
        _.
          execute()
      }
      Ok(views.
        html.main("Woop AssaultCube Match league")(Html(""))(Html(
        result)))
        }
  }
}