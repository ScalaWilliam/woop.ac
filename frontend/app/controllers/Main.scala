package controllers

import java.io.File
import java.net.InetAddress
import java.util.UUID
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
import scala.util.Try

object Main extends Controller {
  import ExecutionContext.Implicits.global
  def using[T <: { def close(): Unit }, V](a: => T)(f: T=> V):V  = {
    val i = a
    try f(i)
    finally i.close()
  }

  def questions = statedSync{
    request => implicit s =>
    Ok(views.html.questions(s))
  }


  def homepage = stated { _ => implicit s =>
    val eventsF = CachedDataSourcePlugin.plugin.getEvents
    val gamesF = CachedDataSourcePlugin.plugin.getGames
    val currentStatesF = getCurrentStates
        for {
          xmlContent <- gamesF
          events <- eventsF
          st <- currentStatesF
        }
          yield
      Ok(views.html.homepage(st, events, Html(xmlContent)))
  }
  lazy val directory = {
    val f= new File(Play.current.configuration.getString("demos.directory").getOrElse(s"${scala.util.Properties.userHome}/demos")).getCanonicalFile
    Logger.info(s"Serving demos from: $f")
    f
  }
  def readDemo(id: Int) = {
    controllers.ExternalAssets.at(directory.getAbsolutePath, s"$id.dmo")
  }

  def readGame(id: Int) = stated { _ => implicit s =>
    for {
      xmlContent <- DataSourcePlugin.plugin.getGame(id.toString)
    }
    yield
        Ok(views.
          html.main("Woop AssaultCube Match league")(Html(""))(Html(xmlContent.body)))
  }

  def viewMe = registeredSync { _ => state =>
    SeeOther(controllers.routes.Main.viewPlayer(state.profile.userId).url) }

  // Only assign a session ID at this point. And give it a session token as well
  def login = Action.async {
    implicit request =>
      val sessionId = request.cookies.get(RegisteredUserManager.SESSION_ID).map(_.value).getOrElse(UUID.randomUUID().toString)
      val sessionCookie = Cookie(RegisteredUserManager.SESSION_ID, sessionId, maxAge = Option(200000))
      val newTokenValue = UUID.randomUUID().toString
      RegisteredUserManager.userManagement.sessionTokens.put(sessionId, newTokenValue)
      val noCacheCookie = Cookie("nocache", "true", maxAge = Option(200000))
      Future { SeeOther(RegisteredUserManager.userManagement.authUrl(newTokenValue)).withCookies(sessionCookie, noCacheCookie) }
  }

  def stated[V](f: Request[AnyContent] => SessionState => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      RegisteredUserManager.userManagement.getSessionState.flatMap { implicit session =>
        f(request)(session)
      }
  }

  lazy val topic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("new-users")

  def statedSync[V](f: Request[AnyContent] => SessionState => Result): Action[AnyContent] =
    stated { a => b =>
      Future{f(a)(b)}
    }
  def registeredSync[V](f: Request[AnyContent] => RegisteredSession => Result): Action[AnyContent] =
    registered { a => b =>
      Future{f(a)(b)}
    }

  def registered[V](f: Request[AnyContent] => RegisteredSession => Future[Result]): Action[AnyContent] =
    stated { implicit request => {
      case SessionState(Some(sessionId), Some(GoogleEmailAddress(email)), Some(profile)) =>
        f(request)(RegisteredSession(sessionId, profile))
      case other =>
        Future {
          SeeOther(controllers.routes.Main.createProfile().url)
        }
    }
    }

  def mainUrl(implicit request : play.api.mvc.RequestHeader) =
    controllers.routes.Main.oauth2callback().absoluteURL()

  def oauth2callback = Action.async {
    implicit request =>
      val code = request.queryString("code").head
      val state = request.queryString("state").head
      val sessionId = request.cookies(RegisteredUserManager.SESSION_ID).value
      val expectedState = RegisteredUserManager.userManagement.sessionTokens.get(sessionId)
      //      RegisteredUserManager.userManagement.sessionEmails.remove(sessionId)
      RegisteredUserManager.userManagement.sessionTokens.remove(sessionId)
      if ( state != expectedState ) {
        throw new RuntimeException(s"Expected $expectedState, got $state")
      }

      for {
        user <- RegisteredUserManager.userManagement.acceptOAuth(code)
      } yield {
        RegisteredUserManager.userManagement.sessionEmails.put(sessionId, user.email)
        SeeOther(controllers.routes.Main.viewMe().absoluteURL())
      }
  }

  val reader = {
    val database = new File(scala.util.Properties.userHome, "GeoLite2-Country.mmdb")
    new DatabaseReader.Builder(database).build()
  }

  def viewPlayers = stated { r => implicit s =>
    for { r <- DataSourcePlugin.plugin.getPlayers
    } yield Ok(views.html.viewPlayers(Html(r.body)))
  }

  def viewPlayer(id: String) = stated {
    r => implicit s =>
      for { stuff <- DataSourcePlugin.plugin.viewUser(id) }
      yield {
        stuff match {
          case None => NotFound
          case Some(xml) =>
            val name = (xml \\ "@name").text
            val profileData = (xml \\ "div").headOption.get
        Ok(views.html.viewProfile(name, Html(profileData.toString())))
      }
      }
  }

  def getCountryCode(ip:String): Option[String] =
    for {
      countryResponse <- Try(reader.country(InetAddress.getByName(ip))).toOption
      country <- Option(countryResponse.getCountry)
      isoCode <- Option(country.getIsoCode)
    } yield isoCode

  def getRegistrationDetail(country: String, email: String, ip:String)(implicit request: Request[AnyContent]): Option[RegistrationDetail] =
    for {
      form <- request.body.asMultipartFormData.map(_.dataParts)
      gameNickname <- form.get("game-nickname").map(_.headOption).flatten
      shortName <- form.get("short-name").map(_.headOption).flatten
      userId <- form.get("user-id").map(_.headOption).flatten
    } yield RegistrationDetail(
        email = email,
        countryCode = country,
        userId = userId,
        shortName = shortName,
        gameNickname = gameNickname, ip = ip
      )
  case class PreventAccess(reason: String) extends Exception
  case class FailRegistration(countryCode: String, reasons: List[String]) extends Exception
  case class InitialPage(countryCode: String) extends Exception
  case class UserRegistered(userId: String) extends Exception
  case class ContinueRegistering(countryCode: String) extends Exception
  case class YouAlreadyHaveAProfile() extends Exception
  def createProfile = stated{implicit request => implicit state =>
    import scala.async.Async.{async, await}
    state match {
      case SessionState(_, None, _) =>
        Future{SeeOther(controllers.routes.Main.login().url)}
      case _ =>
        async {
          if ( state.profile.nonEmpty ) {
            SeeOther(controllers.routes.Main.viewPlayer(state.profile.get.userId).url)
          }
          val ipAddress = if ( scala.util.Properties.osName == "Windows 7" ) { "77.44.45.26" } else request.remoteAddress

          getCountryCode(ipAddress) match {
            case None =>
              Ok(views.html.createProfileNotAllowed(List(s"Could not find a country code for your IP address $ipAddress.")))
            case Some(countryCode) =>
              state.googleEmailAddress match {
                case None => Ok(views.html.createProfileNotAllowed(List("You do not appear to have an e-mail address. Please sign in again.")))
                case Some(GoogleEmailAddress(emailAddress)) =>
                  val ipExists = await(RangerPlugin.awaitPlugin.rangeExists(ipAddress))
                  if ( !ipExists ) {
                    Ok(views.html.createProfileNotAllowed(List(s"You do not appear to have have played with your current IP $ipAddress.")))
                  } else {
                    getRegistrationDetail(countryCode, emailAddress, ipAddress) match {
                      case None =>
                        Ok(views.html.createProfile(countryCode))
                      case Some(reg) =>
                        if ( """.{3,15}""".r.unapplySeq(reg.gameNickname).isEmpty) {
                          Ok(views.html.createProfile(countryCode, List("Invalid game nickname specified")))
                        } else if ( """[A-Za-z0-9]{3,12}""".r.unapplySeq(reg.shortName).isEmpty) {
                          Ok(views.html.createProfile(countryCode, List("Invalid short name specified")))
                        } else if ( """[a-z0-9]{3,10}""".r.unapplySeq(reg.userId).isEmpty) {
                          Ok(views.html.createProfile(countryCode, List("Invalid username specified")))
                        } else {
                          val regDetail = await(RegisteredUserManager.userManagement.registerValidation(reg))
                          regDetail match {
                            case org.scalactic.Bad(stuff) =>
                              Ok(views.html.createProfile(countryCode, stuff.toList))
                            case _ =>
                              await(RegisteredUserManager.userManagement.registerUser(reg))
                              topic.publish(reg.userId)
                              await(AwaitUserUpdatePlugin.awaitPlugin.awaitUser(reg.userId).recover{case _: AskTimeoutException => "whatever"})
                              SeeOther(controllers.routes.Main.viewPlayer(reg.userId).url)
                          }
                        }
                    }
                  }
              }
          }
        }
      case _ => Future { Forbidden }
    }
  }
  import play.api.mvc._
  import play.api.Play.current

  def serversUpdates = WebSocket.acceptWithActor[String, String] { request => out =>
    ServerUpdatesActor.props(out)
  }
  def newGames = WebSocket.acceptWithActor[String, String] { request => out =>
    NewGamesActor.props(out)
  }
  def newUserEvents = WebSocket.acceptWithActor[String, String] { request => out =>
    NewUserEventsActor.props(out)
  }
  import akka.actor._

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
        val gameDataF = for {r <- DataSourcePlugin.plugin.getGame(gameId)
        } yield {r.xml \@ "gameJson"}
        import akka.pattern.pipe
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
  def getCurrentStates: Future[CurrentStates] = {
    import akka.pattern.ask
    implicit val sys = play.api.libs.concurrent.Akka.system
    import concurrent.duration._
    implicit val to = Timeout(5.seconds)
    ask(ServerUpdatesPlugin.serverUpdatesPlugin.act, GiveStates).mapTo[CurrentStates]
  }
  def servers = stated{
    request => implicit s =>
      for { cs <- getCurrentStates }
        yield Ok(views.html.servers(cs))
  }
  def videos = stated { _ => implicit s =>
    val videosF = DataSourcePlugin.plugin.getVideos
    for {
      xmlContent <- videosF
    }
    yield
      Ok(views.html.videos(Html(xmlContent.body)))
  }
}