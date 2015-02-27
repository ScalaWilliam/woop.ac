package plugins

import play.api._
import plugins.DataSourcePlugin.UserProfile
import scala.concurrent.{Future, ExecutionContext}
import scala.xml.{PCData, Text}

class DataSourcePlugin(implicit app: Application) extends Plugin {
  import scala.concurrent.ExecutionContext.Implicits.global
  def getEvents = {
    for { r <- BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[
declare option output:method 'json';
let $events :=
let $now := current-dateTime()
for $event in /user-event[@at-game]
let $user-id := data($event/@user-id)
let $user := /registered-user[@id = $user-id]
let $name := data($user/@game-nickname)
let $game-id := data($event/@at-game)
let $game := /game[@id=$game-id]
let $date := data($game/@date)
let $dateTime := xs:dateTime($date)
let $ago := xs:dayTimeDuration($now - $dateTime)
let $when :=
  if ( $ago le xs:dayTimeDuration("PT1H") ) then ("just now")
  else if ( $ago le xs:dayTimeDuration("PT12H"))  then ("today")
  else if ( $ago le xs:dayTimeDuration("P2D")) then ("yesterday")
  else (days-from-duration($ago)||" days ago")
let $title :=
  if ( $event/became-capture-master-event ) then ("became Capture Master")
  else if ( $event/slaughtered-event ) then ("achieved Butcher")
  else if ( $event/solo-flagger-event ) then ("achieved Maverick")
  else if ( $event/terrible-game ) then ("had a Terrible Game")
  else if ( $event/achieved-dday-event ) then ("had a D-Day")
  else if ( $event/became-flag-master ) then ("became Flag Master")
  else if ( $event/became-frag-master ) then ("became Frag Master")
  else if ( $event/became-cube-addict ) then ("became Cube Addict")
  else if ( $event/became-tdm-lover ) then ("became TDM Lover")
  else if ( $event/became-tosok-lover ) then ("became Lucky Luke")
  else if ( $event/completed-map-event ) then ("completed map "||data($event/completed-map-event/@map))
  else if ( $event/achieved-new-flag-master-level ) then ("achieved Flag Master level "||data($event//@new-level-flags))
  else if ( $event/achieved-new-frag-master-level ) then ("achieved Frag Master level "||data($event//@new-level-frags))
  else if ( $event/achieved-new-cube-addict-level ) then ("achieved Cube Addict level "||data($event//@new-level-hours)||"h")
  else ("?? "||node-name($event/*))
let $text := $name || "  "||$title
where not(empty($text))
order by $date descending
return map { "when": $when, "user": $user-id, "title": $text }

return array { $events[position() = 1 to 7] }
]]>
      </rest:text>
      <rest:variable name="user-id" value="drakas"/>
    </rest:query>) }
    yield r.body
  }
  
  def getPlayers = BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest='http://basex.org/rest'>
    <rest:text><![CDATA[<ol>{
        for $ru in /registered-user
        order by $ru/@id ascending
        return <li><a href="{"/player/"||data($ru/@id)||"/"}">{data($ru/@name)}</a></li>
        }</ol>
        ]]></rest:text></rest:query>)

  def viewUser(userId: String): Future[Option[UserProfile]] = {
    implicit val app = Play.current
    val theXml = <rest:query xmlns:rest="http://basex.org/rest">
      <rest:text>{Text(personProfileXquery)}<![CDATA[
]]>
      </rest:text>
      <rest:variable name="user-id" value={userId}/>
    </rest:query>
    BasexProviderPlugin.awaitPlugin.query(theXml).map(x => Option(x).filter(_.body.nonEmpty).map{
    json =>
      UserProfile(name = json.json.\\("nickname").head.as[String], profileData = json.body)
    })
  }

  def getGame(id: String) = BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text>{PCData(getGameQueryText)}</rest:text>
    <rest:variable name="game-id" value={id.toString}/>
  </rest:query>).map(_.body)

  def getGameX(id: String) = BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text>{PCData(getGameQueryText)}</rest:text>
    <rest:variable name="game-id" value={id.toString}/>
  </rest:query>)

  def getGames = BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text>{PCData(getGameQueryText)}</rest:text>
    <rest:variable name="game-id" value="0"/>
  </rest:query>).map(_.body)


  lazy val processGameXQuery = {
    scala.io.Source.fromInputStream(Play.resourceAsStream("/process-game.xq").get).mkString
  }
  lazy val personProfileXquery = {
    scala.io.Source.fromInputStream(Play.resourceAsStream("/person-profile.xq").get).mkString
  }

  def getVideos = BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text>{Text(processGameXQuery)}<![CDATA[
let $vids :=
  for $approved-video in /video-approved
  let $id := data($approved-video/@id)
  for $video in /video
  where $video/@id = $id
  let $games-ids := $video//game
  order by $video/@published-at descending
  let $uri := "//www.youtube-nocookie.com/embed/"||$id||"?rel=0"
  let $game-ln :=
    for $game in subsequence(/game[@id = $games-ids],1,1)
    let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
    return <p class="game-ln">{local:game-header($game, $has-demo)}</p>
  return <li><iframe width="480" height="270" src="{$uri}" frameborder="0" allowfullscreen="allowfullscreen"><!----></iframe>{$game-ln}</li>
return if ( empty($vids) ) then() else (<ol id="vids">{$vids}</ol>)
      ]]></rest:text>
    <rest:variable name="game-id" value="0"/>
  </rest:query>)

  def getGameQueryText =
    processGameXQuery +
      """
        |declare option output:method 'json';
        |declare variable $game-id as xs:integer external;
        |
        |let $stuffs := 
        |let $games := if ( $game-id = 0 ) then (/game) else (/game[@id=$game-id])
        |let $earliest := (adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P1D"), ())) cast as xs:date
        |let $rus := /registered-user
        |for $game in $games
        |order by $game/@date descending
        |let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
        |let $date := xs:date($dateTime cast as xs:date)
        |where ($game-id != 0) or ($date ge $earliest)
        |let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
        |let $game-item := local:display-game($rus, $game, $has-demo)
        |return $game-item
        |return array{ $stuffs }
        |
      """.stripMargin
  //  """
  //    |
  //    |map { "ok": 1 }
  //  """.stripMargin



}
object DataSourcePlugin {

  case class UserProfile(name: String, profileData: String)
  def plugin: DataSourcePlugin = Play.current.plugin[DataSourcePlugin]
    .getOrElse(throw new RuntimeException("DataSourcePlugin plugin not loaded"))
}