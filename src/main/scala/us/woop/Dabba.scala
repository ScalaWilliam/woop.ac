package us.woop

import org.basex.BaseXServer
import org.basex.core.Context
import org.basex.core.cmd.CreateDB
import org.basex.server.ClientSession

object Dabba extends App {
  val context = new Context()
  val server = new BaseXServer("-p1235")
  val session = new ClientSession("localhost", 1235, "admin", "admin")
//  val result = session.query("1 to 2").execute()
//  println(result)
  new CreateDB("wat", "<test/>").execute(context)
  server.stop()
  //new DropDB("LocalDB").execute(context);
  context.close()
}
