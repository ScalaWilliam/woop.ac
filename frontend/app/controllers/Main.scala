package controllers

import java.io.File

import org.basex.server.ClientSession
import play.api.{Logger, Play}
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
  lazy val processGameXQuery = {
    import play.api.Play.current
    scala.io.Source.fromInputStream(Play.resourceAsStream("/process-game.xq").get).mkString
  }
  def getGameQueryText =
    processGameXQuery +
      """
        |declare variable $game-id as xs:integer external;
        |let $games := if ( $game-id = 0 ) then (/game) else (/game[@id=$game-id])
        |let $earliest := (adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P5D"), ())) cast as xs:date
        |for $game in $games
        |let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
        |let $date := xs:date($dateTime cast as xs:date)
        |where $date ge $earliest
        |where count($game//player) ge 4
        |order by data($game/@date) descending
        |let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
        |return local:display-game($game, $has-demo)
        |
      """.stripMargin

  def read = Action{
    request =>
      withSession { conn =>
//        val header = using(conn.query("/article[@id='top']"))(_.execute())
        val result = using(conn.query(getGameQueryText)) {
        x =>
          x.bind("game-id", 0, "xs:integer")
          x.execute()
      }
      Ok(views.
        html.main("Woop AssaultCube Match league")(Html(""))(Html(
        result)))
        }
  }
  lazy val directory = {
    val f= new File(Play.current.configuration.getString("demos.directory").getOrElse(s"${scala.util.Properties.userHome}/demos")).getCanonicalFile
    Logger.info(s"Serving demos from: $f")
    f
  }
  def readDemo(id: Int) = {
    controllers.ExternalAssets.at(directory.getAbsolutePath, s"$id.dmo")
  }
  def readGame(id: Int) = Action{
    request =>
      withSession { conn =>
//        val header = using(conn.query("/article[@id='top']"))(_.execute())
        val result = using(conn.query(getGameQueryText)) {
        x =>
          x.bind("game-id", id, "xs:integer")
          x.execute()
      }
      Ok(views.
        html.main("Woop AssaultCube Match league")(Html(""))(Html(
        result)))
        }
  }
}