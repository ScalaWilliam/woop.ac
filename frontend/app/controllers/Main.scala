package controllers

import org.basex.server.ClientSession
import play.api.mvc._

object Main extends Controller {
  def using[T <: { def close(): Unit }, V](a: => T)(f: T=> V):V  = {
    val i = a
    try f(i)
    finally i.close()
  }
  def read = Action{
    request =>
      val conn = new ClientSession("odin", 1236, "admin", "admin")
      conn.execute("open acleague")
      val result = using(conn.query("/")) {
        _.execute()
      }

      Ok(result)
  }
}