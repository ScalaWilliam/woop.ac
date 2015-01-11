package controllers

import java.io.File
import java.net.InetAddress
import java.util.UUID

import com.maxmind.geoip2.DatabaseReader
import org.basex.server.ClientSession
import org.scalactic._
import play.api.{Logger, Play}
import play.api.mvc._
import play.twirl.api.Html
import plugins.{HazelcastPlugin, RegisteredUserManager}
import plugins.RegisteredUserManager.{RegisteredSession, GoogleEmailAddress, RegistrationDetail, SessionState}

import scala.concurrent.Future
import scala.util.Try
import org.scalactic._
object Main extends Controller {
  def using[T <: { def close(): Unit }, V](a: => T)(f: T=> V):V  = {
    val i = a
    try f(i)
    finally i.close()
  }
  def readx = Action{
    request =>
      val conn = new ClientSession("odin.duel.gg", 1236, "admin", "admin")
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
  def servers = statedSync{
    request => implicit s =>
              Ok(views.html.servers(s))
  }
  def questions = statedSync{
    request => implicit s =>
    Ok(views.html.questions(s))
  }
  def withSession[T](x: ClientSession => T): T = {
    using(new ClientSession("odin.duel.gg", 1236, "admin", "admin")) { c =>
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

  def read = statedSync { _ => implicit s =>
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
  def readGame(id: Int) = statedSync { _ => implicit s =>
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

  def viewMe = registeredSync { _ => state =>
    SeeOther(controllers.routes.Main.viewPlayer(state.profile.userId).url) }

  // Only assign a session ID at this point. And give it a session token as well
  def login = Action.async {
    implicit request =>
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      val sessionId = request.cookies.get(RegisteredUserManager.SESSION_ID).map(_.value).getOrElse(UUID.randomUUID().toString)
      val sessionCookie = Cookie(RegisteredUserManager.SESSION_ID, sessionId, maxAge = Option(200000))
      val newTokenValue = UUID.randomUUID().toString
      RegisteredUserManager.userManagement.sessionTokens.put(sessionId, newTokenValue)
      val noCacheCookie = Cookie("nocache", "true", maxAge = Option(200000))
      Future { SeeOther(RegisteredUserManager.userManagement.authUrl(newTokenValue)).withCookies(sessionCookie, noCacheCookie) }
  }

  def stated[V](f: Request[AnyContent] => SessionState => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      RegisteredUserManager.userManagement.getSessionState.flatMap { implicit suzzy =>
        f(request)(suzzy)
      }
  }

  lazy val topic = HazelcastPlugin.hazelcastPlugin.hazelcast.getTopic[String]("new-users")

  def statedSync[V](f: Request[AnyContent] => SessionState => Result): Action[AnyContent] =
    stated { a => b =>
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      Future{f(a)(b)}
    }
  def registeredSync[V](f: Request[AnyContent] => RegisteredSession => Result): Action[AnyContent] =
    registered { a => b =>
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      Future{f(a)(b)}
    }

  def registered[V](f: Request[AnyContent] => RegisteredSession => Future[Result]): Action[AnyContent] =
    stated { implicit request => sessionState =>
      sessionState match {
        case SessionState(Some(sessionId), Some(GoogleEmailAddress(email)), Some(profile)) =>
          f(request)(RegisteredSession(sessionId, profile))
        case other =>
          import scala.concurrent.ExecutionContext.Implicits.global
          Future{SeeOther(controllers.routes.Main.createProfile().url)}
      }
    }
//
//  def createProfile = stated { implicit request => implicit suzzy =>
//    suzzy.profile match {
//      case Some(_) => Future {
//        Ok("You already have a profile, naughty!")
//      }
//      case _ => Future {
//        Ok(views.html.second.createProfile(profileForm))
//      }
//    }
//  }
//
//  def logout = Action {
//    implicit request =>
//      request.cookies.get(RegisteredUserManager.SESSION_ID).map(_.value).foreach(RegisteredUserManager.userManagement.sessionEmails.remove)
//      TemporaryRedirect(controllers.routes.Main.index().absoluteURL())
//  }

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

      import play.api.libs.concurrent.Execution.Implicits.defaultContext
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

  def viewPlayer(id: String) = stated {
    r => implicit s =>

      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      for { stuff <- RegisteredUserManager.userManagement.viewUser(id) }
      yield {
        stuff match {
          case None => NotFound
          case Some(xml) =>
            val name = (xml \\ "@name").text
            val profileData = (xml \\ "div").headOption.get
        Ok(views.html.viewProfile(name, Html(profileData.toString)))
      }
      }
  }

  def getCountryCode(implicit request: Request[_]): Option[String] =
    for {
      countryResponse <- Try(reader.country(InetAddress.getByName(request.remoteAddress))).toOption
      country <- Option(countryResponse.getCountry)
      isoCode <- Option(country.getIsoCode)
    } yield isoCode

  def getRegistrationDetail(country: String, email: String)(implicit request: Request[AnyContent]): Option[RegistrationDetail] =
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
        gameNickname = gameNickname, ip = request.remoteAddress
      )
  case class PreventAccess(reason: String) extends Exception
  case class FailRegistration(countryCode: String, reasons: List[String]) extends Exception
  case class InitialPage(countryCode: String) extends Exception
  case class UserRegistered(userId: String) extends Exception
  case class ContinueRegistering(countryCode: String) extends Exception
  case class YouAlreadyHaveAProfile() extends Exception
  def createProfile = stated{implicit request => implicit state =>
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    state match {
      case SessionState(_, None, _) =>
        Future{SeeOther(controllers.routes.Main.login().url)}
      case _ =>
        val registerMe = for {
          _ <- Future {
            if (state.profile.nonEmpty) {
              throw new YouAlreadyHaveAProfile()
            }
          }
          countryCode = getCountryCode match {
            case Some(c) => c
            case None => throw new PreventAccess(s"Could not find a country code for your IP address ${request.remoteAddress}.")
          }
          email = state.googleEmailAddress match {
            case Some(e) => e
            case _ => throw new PreventAccess("You do not appear to have an e-mail address. Please sign in.")
          }
          ipExists <- RegisteredUserManager.userManagement.ipExists(request.remoteAddress)
          _ = if (!ipExists) { throw new PreventAccess("You do not appear to have have played with your current IP.") } else Unit
        regDetail = getRegistrationDetail(countryCode, email.email)
          validated <- regDetail match {
            case Some(reg) =>
              if ( """.{3,15}""".r.unapplySeq(reg.gameNickname).isEmpty ) {
                throw new FailRegistration(countryCode,List("Invalid game nickname specified"))
              }
              if ("""[A-Za-z0-9]{3,12}""".r.unapplySeq(reg.shortName).isEmpty) {
                throw new FailRegistration(countryCode,List("Invalid short name specified"))
              }
              if ( """[a-z0-9]{3,10}""".r.unapplySeq(reg.userId).isEmpty) {
                throw new FailRegistration(countryCode,List("Invalid username specified"))
              }
              RegisteredUserManager.userManagement.registerValidation(reg)
            case None => throw new InitialPage(countryCode)
          }
          _ = validated match {
            case org.scalactic.Bad(stuff) =>
              throw new FailRegistration(countryCode,stuff.toList)
            case _ => Unit
          }
          reg <- RegisteredUserManager.userManagement.registerUser(regDetail.get)
        } yield {
          throw new UserRegistered(regDetail.get.userId)
        }
        for {
          ex <- registerMe.recover{case t => t }
        } yield ex match {
          case YouAlreadyHaveAProfile() =>
            SeeOther(controllers.routes.Main.viewPlayer(state.profile.get.userId).url)
          case PreventAccess(reason) =>
            Ok(views.html.createProfileNotAllowed(List(reason)))
          case FailRegistration(countryCode, reasons) =>
            Ok(views.html.createProfile(countryCode, reasons))
          case InitialPage(countryCode)=>
            Ok(views.html.createProfile(countryCode))
          case UserRegistered(userId) =>
            topic.publish(userId)
            SeeOther(controllers.routes.Main.viewPlayer(userId).url)
          case ContinueRegistering(countryCode) =>
            Ok(views.html.createProfile(countryCode))
          case other =>
            throw other
        }
      case _ => Future { Forbidden }
    }
  }

}