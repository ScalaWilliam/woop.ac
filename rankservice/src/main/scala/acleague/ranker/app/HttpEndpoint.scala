package acleague.ranker.app

import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp

import scala.util.Try


trait HttpEndpoint extends App with SimpleRoutingApp {
  this: App =>

  implicit def as: ActorSystem

  def manifestGitSha = {
    Try {
      Option(new java.util.jar.Manifest(getClass.getClassLoader.getResourceAsStream("META-INF/MANIFEST.MF")).getMainAttributes.getValue("Git-Head-Rev"))
    }.toOption.flatten.getOrElse("")
  }

  startServer(interface = "0.0.0.0", port = 34514) {
    path("version" / PathEnd) { get { complete { manifestGitSha } } }
  }

}
