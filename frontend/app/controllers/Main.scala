package controllers

import java.io._
import java.net.InetAddress
import java.nio.file.Paths
import java.util.UUID
import javax.script._
import jdk.nashorn.api.scripting.NashornScriptEngine
import play.api.libs.json.Json
import plugins.DataSourcePlugin.UserProfile
import plugins.NewGamesPlugin.GotNewGame
import akka.pattern.AskTimeoutException
import akka.util.Timeout
import com.maxmind.geoip2.DatabaseReader
import play.api.{Logger, Play}
import play.api.mvc._
import play.twirl.api.Html
import plugins.NewUserEventsPlugin.UpdatedUserEvents
import plugins.ServerUpdatesPlugin.{GiveStates, CurrentStates, ServerState}
import plugins._
import plugins.RegisteredUserManager.{RegisteredSession, GoogleEmailAddress, RegistrationDetail, SessionState}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{DynamicVariable, Try}
import scala.async.Async.{async, await}
import javax.inject._


@Singleton
class JSRenderPool() {
  def inputData: InputStream = {
      val localPath = Paths.get("frontend-js", "build", "whut.js")
      if (localPath.toFile.exists()) {
        new FileInputStream(localPath.toFile)
      } else {
        getClass.getResourceAsStream("/whut.js")
      }
  }
  val engine = {
    val engine = new ScriptEngineManager(null).getEngineByName("nashorn").asInstanceOf[NashornScriptEngine]
    engine.eval("var global = this;")
    engine.eval("var console = {}; console.debug = print; console.warn = print; console.log = print;")
    engine.eval(new InputStreamReader(inputData))
    engine
  }
  def execute(inputJson: String): String = {
    try engine.asInstanceOf[Invocable].invokeFunction("RenderMe", inputJson).asInstanceOf[String]
    catch {
      case e: Throwable =>
        throw new RuntimeException(s"Failed to run script due to: $e. Script: $inputJson", e)
    }
  }
}


class Main @Inject()
(dataSource: CachedDataSourcePlugin,
hazelcastPlugin: HazelcastPlugin,
val registeredUserManager: RegisteredUserManager,
serverUpdatesPlugin: ServerUpdatesPlugin,
dataSourcePlugin: DataSourcePlugin,
rangerPlugin: RangerPlugin,
awaitUserUpdatePlugin: AwaitUserUpdatePlugin,
renderer: JSRenderPool
) extends Controller with SysActions {

  import ExecutionContext.Implicits.global

  def questions = statedSync {
    request => implicit s =>
    Ok(views.html.questions(s))
  }

//  def dataSource = if ( scala.util.Properties.isWin ) DataSourcePlugin.plugin else CachedDataSourcePlugin.plugin

  def homepage = stated { _ => implicit s =>
    val eventsF = dataSource.getEvents
    val gamesF = dataSource.getGames
    val currentStatesF = serverUpdatesPlugin.getCurrentStatesJson
        for {
          xmlContent <- gamesF
          events <- eventsF
          st <- currentStatesF
        }
          yield {
            Ok(views.html.homepage(st, events, xmlContent, Html(renderer.execute(xmlContent))))
          }
  }

  def readGame(id: Int) = stated { _ => implicit s =>
    for {
      jsonContent <- dataSource.getGame(id.toString)
    }
    yield Ok(views.html.viewGame(jsonContent))
  }

  def viewMe = registeredSync { _ => state =>
    SeeOther(controllers.routes.Main.viewPlayer(state.profile.userId).url) }

  def mainUrl(implicit request : play.api.mvc.RequestHeader) =
    controllers.routes.Accounting.oauth2callback().absoluteURL()


  def viewPlayers = stated { r => implicit s =>
    for { r <- dataSourcePlugin.getPlayers
    } yield Ok(views.html.viewPlayers(Html(r.body)))
  }

  def viewPlayer(id: String) = stated {
    r => implicit s =>
      for { stuff <-
//            Future.successful(Option(UserProfile("John", profileData = """{"name": "John"}""")))
            dataSource.viewUser(id)
      }
      yield
        stuff match {
          case None => NotFound
          case Some(UserProfile(name, profileData)) =>
            val profileDataHtml = Html(renderer.execute(profileData))
            Ok(views.html.viewProfile(name, profileDataHtml))
        }
  }

  import play.api.mvc._
  import play.api.Play.current

  import akka.actor._

  def servers = stated{
    request => implicit s =>
      for { cs <- serverUpdatesPlugin.getCurrentStatesJson }
        yield Ok(views.html.servers(cs))
  }
  def videos = stated { _ => implicit s =>
    val videosF = dataSourcePlugin.getVideos
    for {
      xmlContent <- videosF
    }
    yield
      Ok(views.html.videos(Html(xmlContent.body)))
  }

  def version = Action {
    r =>
    def manifestGitSha = {
       Try {
            Option(new java.util.jar.Manifest(getClass.getClassLoader.getResourceAsStream("META-INF/MANIFEST.MF")).getMainAttributes.getValue("Git-Head-Rev"))
          }.toOption.flatten.getOrElse("")
        }
    Ok(manifestGitSha)
  }
}