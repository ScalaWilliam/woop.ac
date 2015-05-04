package controllers

import java.io.File
import java.net.InetAddress
import java.util.UUID
import javax.inject.Inject

import akka.pattern.AskTimeoutException
import com.maxmind.geoip2.DatabaseReader
import play.api.libs.json.Json
import play.api.mvc._
import plugins.RegisteredUserManager.{GoogleEmailAddress, SessionState, RegistrationDetail}
import plugins._

import scala.async.Async._
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

class Accounting @Inject()
(dataSource: CachedDataSourcePlugin,
 hazelcastPlugin: HazelcastPlugin,
 val registeredUserManager: RegisteredUserManager,
 serverUpdatesPlugin: ServerUpdatesPlugin,
 dataSourcePlugin: DataSourcePlugin,
 rangerPlugin: RangerPlugin,
 awaitUserUpdatePlugin: AwaitUserUpdatePlugin
  ) extends Controller with SysActions {

  implicit def mainUrl(implicit request: RequestHeader): MainUrl = MainUrl(controllers.routes.Accounting.oauth2callback().absoluteURL())

  // Only assign a session ID at this point. And give it a session token as well
  def login = Action.async {
    implicit request =>
      val sessionId = request.cookies.get(RegisteredUserManager.SESSION_ID).map(_.value).getOrElse(UUID.randomUUID().toString)
      val sessionCookie = Cookie(RegisteredUserManager.SESSION_ID, sessionId, maxAge = Option(200000))
      val newTokenValue = UUID.randomUUID().toString
      registeredUserManager.sessionTokens.put(sessionId, newTokenValue)
      val noCacheCookie = Cookie("nocache", "true", maxAge = Option(200000))

      Future { SeeOther(registeredUserManager.authUrl(newTokenValue)).withCookies(sessionCookie, noCacheCookie) }
  }

  lazy val topic = hazelcastPlugin.hazelcast.getTopic[String]("new-users")

  def oauth2callback = Action.async {
    implicit request =>
      val code = request.queryString("code").head
      val state = request.queryString("state").head
      val sessionId = request.cookies(RegisteredUserManager.SESSION_ID).value
      val expectedState = registeredUserManager.sessionTokens.get(sessionId)
      //      registeredUserManager.sessionEmails.remove(sessionId)
      registeredUserManager.sessionTokens.remove(sessionId)
      if ( state != expectedState ) {
        throw new RuntimeException(s"Expected $expectedState, got $state")
      }

      for {
        user <- registeredUserManager.acceptOAuth(code)
      } yield {
        registeredUserManager.sessionEmails.put(sessionId, user.email)
        SeeOther(controllers.routes.Main.viewMe().absoluteURL())
      }
  }

  val reader = {
    val database = new File(scala.util.Properties.userHome, "GeoLite2-Country.mmdb")
    new DatabaseReader.Builder(database).build()
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
  def createProfile = stated{implicit request =>                             implicit state =>

    state match {
      case SessionState(_, None, _) =>
        Future{SeeOther(controllers.routes.Accounting.login().url)}
      case SessionState(_, _, Some(profile)) =>
        Future{SeeOther(controllers.routes.Main.viewPlayer(profile.userId).url)}
      case _ =>
        async {
          val ipAddress = if ( scala.util.Properties.osName == "Windows 7" ) { "77.44.45.26" } else request.remoteAddress

          getCountryCode(ipAddress) match {
            case None =>
              Ok(views.html.createProfileNotAllowed(List(s"Could not find a country code for your IP address $ipAddress.")))
            case Some(countryCode) =>
              state.googleEmailAddress match {
                case None => Ok(views.html.createProfileNotAllowed(List("You do not appear to have an e-mail address. Please sign in again.")))
                case Some(GoogleEmailAddress(emailAddress)) =>
                  val ipExists = await(rangerPlugin.rangeExists(ipAddress))
                  if ( !ipExists ) {
                    Ok(views.html.createProfileNotAllowed(List(s"You do not appear to have have played with your current IP $ipAddress.")))
                  } else {
                    getRegistrationDetail(countryCode, emailAddress, ipAddress) match {
                      case None =>
                        Ok(views.html.createProfile(countryCode))
                      case Some(reg) =>
                        if ( """.{3,15}""".r.unapplySeq(reg.gameNickname).isEmpty) {
                          Ok(views.html.createProfile(countryCode, List("Invalid game nickname specified")))
                        } else if ( """[A-Za-z]{3,12}""".r.unapplySeq(reg.shortName).isEmpty) {
                          Ok(views.html.createProfile(countryCode, List("Invalid short name specified")))
                        } else if ( """[a-z]{3,10}""".r.unapplySeq(reg.userId).isEmpty) {
                          Ok(views.html.createProfile(countryCode, List("Invalid username specified")))
                        } else {
                          val regDetail = await(registeredUserManager.registerValidation(reg))
                          regDetail match {
                            case org.scalactic.Bad(stuff) =>
                              Ok(views.html.createProfile(countryCode, stuff.toList))
                            case _ =>
                              await(registeredUserManager.registerUser(reg))
                              topic.publish(reg.userId)
                              await(awaitUserUpdatePlugin.awaitUser(reg.userId).recover{case _: AskTimeoutException => "whatever"})
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


  def settings = registered {
    request => implicit s =>
      async { Ok(views.html.settings()) }
  }

  def settingsIssueKey = registered {
    request => implicit s =>
      async {
        val userId = s.profile.userId
        await(registeredUserManager.issueAuthUser(s.profile.userId))
        val keySeq = await(registeredUserManager.getAuthKey(s.profile.userId)).map(k => "key" -> k).toMap
        Ok(Json.toJson(keySeq))
      }
  }
}
