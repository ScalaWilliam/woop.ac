package controllers

import akka.actor.{Props, ActorRef}
import play.api.Application
import play.api.mvc.WebSocket
import plugins.DataSourcePlugin
import plugins.NewGamesPlugin.GotNewGame
import plugins.NewUserEventsPlugin.UpdatedUserEvents
import plugins.ServerUpdatesPlugin.{GiveStates, CurrentStates, ServerState}
import javax.inject._
import akka.pattern.pipe
class Streamed @Inject()(application: Application, dataSourcePlugin: DataSourcePlugin) {

  implicit def app: Application = application

  def serversUpdates = WebSocket.acceptWithActor[String, String] { request => out =>
    ServerUpdatesActor.props(out)
  }
  def newGames = WebSocket.acceptWithActor[String, String] { request => out =>
    NewGamesActor.props(out)
  }
  def newUserEvents = WebSocket.acceptWithActor[String, String] { request => out =>
    NewUserEventsActor.props(out)
  }
  object ServerUpdatesActor {
    def props(out: ActorRef) = Props(new ServerUpdatesActor(out))
  }
  import akka.actor.ActorDSL._
  class ServerUpdatesActor(out: ActorRef) extends Act {
    whenStarting {
      context.system.eventStream.subscribe(self, classOf[ServerState])
      context.actorSelection("/user/server-updates") ! GiveStates
    }
    become {
      case ServerState(server, json) => out ! json
      case CurrentStates(map) =>
        map.valuesIterator.foreach(str => out ! str)
    }
  }

  object NewGamesActor {
    def props(out: ActorRef) = Props(new NewGamesActor(out))
  }
  class NewGamesActor(out: ActorRef) extends Act {
    whenStarting {
      context.system.eventStream.subscribe(self, classOf[GotNewGame])
    }
    become {
      case GotNewGame(gameId) =>
        val gameDataF = for {r <- dataSourcePlugin.getGameX(gameId)
        } yield {r.xml \@ "gameJson"}
        gameDataF pipeTo out
    }
  }

  object NewUserEventsActor {
    def props(out: ActorRef) = Props(new NewUserEventsActor(out))
  }
  class NewUserEventsActor(out: ActorRef) extends Act {
    whenStarting {
      context.system.eventStream.subscribe(self, classOf[UpdatedUserEvents])
    }
    become {
      case UpdatedUserEvents(updatedJson) =>
        out ! updatedJson
    }
  }
}
