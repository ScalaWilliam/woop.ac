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
import scala.xml.{PCData, Text}

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

  def viewUser(userId: String)(implicit ec: ExecutionContext): Future[Option[scala.xml.Elem]] = {
    implicit val app = Play.current
    val theXml = <rest:query xmlns:rest="http://basex.org/rest">
      <rest:text>{Text(processGameXQuery)}<![CDATA[

declare variable $user-id as xs:string external;
for $u in /registered-user[@id = $user-id]
let $user-record := /user-record[@id= string($u/@id)]
let $record := $user-record
return <player>{$u}<profile><div class="profile">
<h1>{data($u/@game-nickname)}</h1>
{
let $basics-table :=
  if ( empty($user-record) )
  then (<p>No statistics generated yet...</p>)
  else
  <table class="basic-counts">
    <tr>
      <th>Time played</th>
      <td>{
      let $time := xs:duration(data($record/counts/@time))
      let $days := days-from-duration($time)
      let $hours := hours-from-duration($time)
      return
        if ( $days = 0 and $hours = 0 )
        then ("Not enough")
        else (
          if ( $days eq 1 ) then ("1 day")
          else if ( $days gt 1 ) then ($days || " days")
          else ()," ",
          if ( $hours eq 1 ) then ("1 hour")
          else if ( $hours gt 1 ) then ($hours|| " hours")
          else ()
        )
      }</td>
      <th>Flags</th><td>{data($record/counts/@flags)}</td>
    </tr>
  <tr><th>Games played</th><td>{data($record/counts/@games)}</td><th>Frags</th><td>{data($record/counts/@frags)}</td></tr>
  </table>

let $capture-achievement-bar :=
  let $achievement := $record/achievements/capture-master
  return
<achievement-card
  achieved="{data($achievement/@achieved)}"
  achievementTitle="Capture Master"
  type="capture-master"
  >
  {
  if ( $achievement/@achieved = "true" ) then (
  (
  attribute atGame { data(/game[@id = data($achievement/@at-game)]/@date) }
  )
  ) else ((
  attribute totalInLevel { data($achievement/@target) },
  attribute progressInLevel { data($achievement/@progress) },
  attribute remainingInLevel { data($achievement/@remaining) }
  ))
  }<!----></achievement-card>

let $progress-achievements :=
for $achievement in ($record/achievements/flag-master, $record/achievements/frag-master, $record/achievements/cube-addict)
let $level := data($achievement/@level)
return <achievement-card
  achieved="{data($achievement/@achieved)}"
  achievementTitle="{
      if ( $achievement/self::flag-master ) then ("Flag Master: "||$level)
      else if ( $achievement/self::frag-master ) then ("Frag Master: "||$level)
      else if ($achievement/self::cube-addict) then ("Cube Addict: "||$level||"h")
      else (node-name($achievement))
   }"
  type="{node-name($achievement)}"
  achievement-id="{node-name($achievement) ||"-"||data($achievement/@level)}"
  >
  {
  if ( $achievement/@achieved = "true" ) then (attribute when { data(/game[@id = data($achievement/@at-game)]/@date) }) else ((
  attribute totalInLevel { data($achievement/@total-in-level) },
  attribute progressInLevel { data($achievement/@progress-in-level) },
  attribute remainingInLevel { data($achievement/@remaining-in-level) },
  attribute level { data($achievement/@level) }

  ))
  }<!----></achievement-card>

  let $simple-achievements := (
    let $solo-flagger := $record/achievements/solo-flagger
    return <achievement-card
      type="solo-flagger"
      achievementTitle="Solo Flagger"
      description="Achieve all winning team's flags, 5 minimum"
      achieved="{data($solo-flagger/@achieved)}">{
      if ( $solo-flagger/@achieved = "true" ) then (attribute when { /game[@id = data($solo-flagger/@at-game)]/@date }) else (

      )
      }<!----></achievement-card>
    ,
    let $slaughterer := $record/achievements/slaughterer
    return <achievement-card
      type="slaughterer"
      achievementTitle="Slaughterer"
      description="Make at least 80 frags in a game."
      achieved="{data($slaughterer/@achieved)}">{
      if ( $slaughterer/@achieved = "true" ) then (attribute when { /game[@id = data($slaughterer/@at-game)]/@date }) else (

      )
      }<!----></achievement-card>
    ,
    let $dday := $record/achievements/slaughterer
    return <achievement-card
      type="dday"
      achievementTitle="D-Day"
      description="Play at least 12 games in one day."
      achieved="{data($dday/@achieved)}">{
      if ( $dday/@achieved = "true" ) then (attribute when { /game[@id = data($dday/@at-game)]/@date }) else (

      )
      }<!----></achievement-card>
      ,
    let $tdm-lover := $record/achievements/tdm-lover
    return <achievement-card
    type="tdm-lover"
    achieved="{data($tdm-lover/@achieved)}"
    achievementTitle="TDM Lover"
    description="{"Play at least " ||data($tdm-lover/@target)||" TDM games."}">{
      if ( $tdm-lover/@achieved = "true" ) then (attribute when { /game[@id = data($tdm-lover/@at-game)]/@date }) else (
      (
        attribute totalInLevel { data($tdm-lover/@target) },
        attribute progressInLevel { data($tdm-lover/@progress) },
        attribute remainingInLevel { data($tdm-lover/@remain) }
        )
      )
      }<!----></achievement-card>
      ,
    let $tosok-lover := $record/achievements/tosok-lover
    return <achievement-card
    type="tosok-lover"
    achieved="{data($tosok-lover/@achieved)}"
    achievementTitle="TOSOK Lover"
    description="{"Play at least " ||data($tosok-lover/@target)||" TOSOK games."}">{
      if ( $tosok-lover/@achieved = "true" ) then (attribute when { /game[@id = data($tosok-lover/@at-game)]/@date }) else (
(
        attribute totalInLevel { data($tosok-lover/@target) },
        attribute progressInLevel { data($tosok-lover/@progress) },
        attribute remainingInLevel { data($tosok-lover/@remain) }
        )
      )
      }<!----></achievement-card>

  )

  let $achievements :=
  <div class="achievements">
  {$capture-achievement-bar}
  {
  for $a in ($simple-achievements, $progress-achievements)
  order by not(empty($a/@progressInLevel)) descending, $a/@when descending
  return $a

  }

   </div>

let $master-table :=
  <table class="map-master"><thead><tr><th>Map</th><th>RVSF</th><th>CLA</th></tr></thead>
  <tbody>{
    let $map-master := $user-record/achievements/capture-master
    for $completion in $map-master/map-completion
    let $is-completed := $completion/@is-completed = "true"
    return
      <tr class="{if ( $is-completed ) then ("complete") else ("incomplete")}">
        <th>{data($completion/@mode)} @ {data($completion/@map)}</th>
        {
          if ( $completion/@progress-rvsf = 0 ) then (<td class="rvsf incomplete">0/{data($completion/@target-rvsf)}</td>)
          else if ( $is-completed ) then (<td class="rvsf complete">{data($completion/@target-rvsf)}/{data($completion/@target-rvsf)}</td>)
          else if ( $completion/@progress-rvsf = $completion/@target-rvsf ) then (<td class="rvsf complete">{data($completion/@target-rvsf)}/{data($completion/@target-rvsf)}</td>)
          else (<td class="rvsf partial">{data($completion/@progress-rvsf)}/{data($completion/@target-rvsf)}</td>)
        }
        {if ( $completion/@progress-cla = 0 ) then (<td class="cla incomplete">0/{data($completion/@target-cla)}</td>)
          else if ( $is-completed ) then (<td class="cla complete">{data($completion/@target-cla)}/{data($completion/@target-cla)}</td>)
          else if ( $completion/@progress-cla = $completion/@target-cla ) then (<td class="cla complete">{data($completion/@target-cla)}/{data($completion/@target-cla)}</td>)
          else (<td class="cla partial">{data($completion/@progress-cla)}/{data($completion/@target-cla)}</td>)
        }
      </tr>
  }
  </tbody></table>

return
  <div class="main-info">
    <div class="basics">
      {$basics-table}
    </div>
    <div class="achievements">
    {$achievements}
    </div>
    <div class="master">
      <p>Become the <strong>Capture Master</strong> by playing these maps.</p>
      {$master-table}
    </div>
  </div>
}

<h2>Recent games</h2>
{
let $subs := subsequence(
  for $pig in $record//played-in-game
  for $game in /game[@id= data($pig/@game-id)]
  order by $game/@date descending
  let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
  return <li>{local:game-header($game, $has-demo)}</li>,1,7)
return <ol class="recent-games">{$subs}</ol>
}

</div></profile></player>]]>
      </rest:text>
      <rest:variable name="user-id" value={userId}/>
    </rest:query>
    BasexProviderPlugin.awaitPlugin.query(theXml).map(x => Option(x).filter(_.body.nonEmpty).map(_.xml))

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
        |let $earliest := (adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P2D"), ())) cast as xs:date
        |let $rus := /registered-user
        |for $game in $games
        |order by $game/@date descending
        |let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
        |let $date := xs:date($dateTime cast as xs:date)
        |where ($game-id != 0) or ($date ge $earliest)
        |let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
        |let $game-item := local:display-game($rus, $game, $has-demo)
        |return <game-card gameJson="{json:serialize($game-item)}">{comment { "loading..." }}</game-card>
        |
        |
      """.stripMargin
//  """
//    |
//    |map { "ok": 1 }
//  """.stripMargin

  def homepage = stated { _ => implicit s =>
    import Play.current
    val eventsF = NewUserEventsPlugin.newUserEventsPlugin.getEvents
    val gamesF = BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text>{PCData(getGameQueryText)}</rest:text>
      <rest:variable name="game-id" value="0"/>
    </rest:query>)
    val currentStatesF = getCurrentStates
        for {
          xmlContent <- gamesF
          events <- eventsF
          st <- currentStatesF
        }
          yield
      Ok(views.html.homepage(st, events, Html(xmlContent.body)))
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
      xmlContent <- BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
        <rest:text>{PCData(getGameQueryText)}</rest:text>
        <rest:variable name="game-id" value={id.toString}/>
      </rest:query>)
    }
    yield
        Ok(views.
          html.main("Woop AssaultCube Match league")(Html(""))(Html(xmlContent.xml.toString)))
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

  def viewPlayers = stated { r => implicit s =>
    for { r <- BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest='http://basex.org/rest'>
      <rest:text><![CDATA[<ol>{
        for $ru in /registered-user
        order by $ru/@id ascending
        return <li><a href="{"/player/"||data($ru/@id)||"/"}">{data($ru/@game-nickname)}</a></li>
        }</ol>
        ]]></rest:text></rest:query>)
    } yield Ok(views.html.viewPlayers(Html(r.body)))
  }

  def viewPlayer(id: String) = stated {
    r => implicit s =>

      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      for { stuff <- viewUser(id) }
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
      import scala.concurrent.duration._
    }
    become {
      case GotNewGame(gameId) =>
        val gameDataF = for {r <- BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
          <rest:text>
            {PCData(getGameQueryText)}
          </rest:text>
          <rest:variable name="game-id" value={gameId}/>
        </rest:query>)
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
}