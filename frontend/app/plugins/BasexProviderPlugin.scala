package plugins

import akka.actor.{Kill, ActorRef}
import play.api.libs.concurrent.Akka
import play.api.libs.ws.{WSClient, WSAuthScheme, WS}
import plugins.AwaitUserUpdatePlugin.{WaitForUser, UserUpdated}
import akka.pattern.ask
import concurrent.duration._
import scala.concurrent._
import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import akka.actor.ActorDSL._

import scala.xml.Elem
import javax.inject._
@Singleton
class BasexProviderPlugin @Inject()(application: Application, ws: WSClient) {
  implicit def app: Application = application
  def url = Play.current.configuration.getString("basex.url").getOrElse{throw new RuntimeException("No basex url!")}
  def username = Play.current.configuration.getString("basex.username").getOrElse{throw new RuntimeException("No basex username!")}
  def password = Play.current.configuration.getString("basex.password").getOrElse{throw new RuntimeException("No basex password!")}

  def query(xml: Elem) = {
    import ExecutionContext.Implicits.global
    for {
      r <- WS.url(url).withAuth(username, password, WSAuthScheme.BASIC).post(xml)
      _ = if ( r.status != 200 ) { throw new RuntimeException(s"Expected status 200, got ${r.status}. Content: ${r.body}, query: $xml")}
    } yield r
  }

  // todo onstart - check that it connects

}

object BasexProviderPlugin {
}