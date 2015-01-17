package plugins

import akka.actor.{Kill, ActorRef}
import play.api.libs.concurrent.Akka
import play.api.libs.ws.{WSAuthScheme, WS}
import plugins.AwaitUserUpdatePlugin.{WaitForUser, UserUpdated}
import akka.pattern.ask
import concurrent.duration._
import scala.concurrent._
import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import play.api._
import akka.actor.ActorDSL._

import scala.xml.Elem

class BasexProviderPlugin(implicit app: Application) extends Plugin {

  def url = Play.current.configuration.getString("basex.url").getOrElse{throw new RuntimeException("No basex url!")}
  def username = Play.current.configuration.getString("basex.username").getOrElse{throw new RuntimeException("No basex username!")}
  def password = Play.current.configuration.getString("basex.password").getOrElse{throw new RuntimeException("No basex password!")}

  def query(xml: Elem) = {
    WS.url(url).withAuth(username, password, WSAuthScheme.BASIC).post(xml)
  }

  // todo onstart - check that it connects

}

object BasexProviderPlugin {
  def awaitPlugin: BasexProviderPlugin = Play.current.plugin[BasexProviderPlugin]
    .getOrElse(throw new RuntimeException("BasexProviderPlugin plugin not loaded"))
}