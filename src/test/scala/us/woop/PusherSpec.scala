package us.woop

import java.util.UUID
import org.basex.BaseXServer
import org.basex.core.cmd.{XQuery, Open, DropDB, CreateDB}
import org.basex.server.ClientSession
import org.scalatest.{Matchers, WordSpec}
import us.woop.ServerProcessor.GameXmlReady

class PusherSpec extends WordSpec with Matchers {
  "Pusher" must {
    "Push properly" in {
      val server = new BaseXServer("-p12391")
      val session = new ClientSession("localhost", 12391, "admin", "admin")
      try {
        session.execute(new CreateDB("for-testing"))
        session.execute(new Open("for-testing"))
        val id = UUID.randomUUID().toString
        val inXml = <test-string id={id}/>.toString
        MessageWriter.publishMessage(session)(GameXmlReady(inXml))
        val result = session.execute(new XQuery(s"//test-string[@id='$id']"))
        result shouldBe inXml
        session.execute(new DropDB("for-testing"))
      } finally {
        try {
          session.close()
        } finally {
          server.stop()
        }
      }
    }
  }
}
