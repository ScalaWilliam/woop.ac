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
        return <li><a href="{"/player/"||data($ru/@id)||"/"}">{data($ru/@game-nickname)}</a></li>
        }</ol>
        ]]></rest:text></rest:query>)

  def viewUser(userId: String): Future[Option[UserProfile]] = {
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

let $capture-achievement-bar :=
  let $achievement := $record/achievements/capture-master
  return
<achievement-card
  achieved="{data($achievement/@achieved)}"
  achievementTitle="Capture Master"
  type="capture-master"
  description="Complete the selected CTF maps, both sides 3 times"
  >
  {
  if ( $achievement/@achieved = "true" ) then (
  (
  attribute when { data(/game[@id = data($achievement/@at-game)]/@date) }
  )
  ) else ((
  attribute totalInLevel { data($achievement/@target) },
  attribute progressInLevel { data($achievement/@progress) },
  attribute remainingInLevel { data($achievement/@remaining) }
  ))
  }  <div class="master">
      {$master-table}
    </div>
    </achievement-card>

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
   description="{
   if ($achievement/self::frag-master[@level='500']) then ("Well, that's a start.")
   else if ($achievement/self::frag-master[@level='1000']) then ("Already lost count.")
   else if ($achievement/self::frag-master[@level='2000']) then ("I'm quite good at this!")
   else if ($achievement/self::frag-master[@level='5000']) then ("I've seen blood.")
   else if ($achievement/self::frag-master[@level='10000']) then ("That Rambo guy got nothin' on me.")
   else if ($achievement/self::flag-master[@level='50']) then ("What's that blue thing?")
   else if ($achievement/self::flag-master[@level='100']) then ("I'm supposed to bring this back?")
   else if ($achievement/self::flag-master[@level='200']) then ("What do you mean it's TDM?")
   else if ($achievement/self::flag-master[@level='500']) then ("Yeah, I know where it goes.")
   else if ($achievement/self::flag-master[@level='1000']) then ("Can I keep one at least?")
   else if ($achievement/self::cube-addict[@level='5']) then ("Hey, this game looks fun.")
   else if ($achievement/self::cube-addict[@level='10']) then ("I kinda like this game.")
   else if ($achievement/self::cube-addict[@level='20']) then ("Not stopping now!")
   else if ($achievement/self::cube-addict[@level='50']) then ("I love this game!")
   else if ($achievement/self::cube-addict[@level='100']) then ("Just how many hours??")
   else if ($achievement/self::cube-addict[@level='200']) then ("Wait, when did I start?")
   else ()
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
      type="maverick"
      achievementTitle="Maverick"
      description="Achieve all winning team's flags, 5 minimum"
      achieved="{data($solo-flagger/@achieved)}">{
      if ( $solo-flagger/@achieved = "true" ) then (attribute when { /game[@id = data($solo-flagger/@at-game)]/@date }) else (

      )
      }<!----></achievement-card>
    ,
    let $terrible-game := $record/achievements/terrible-game
    return <achievement-card
      type="terrible-game"
      achievementTitle="Terrible Game"
      description="Score less than 15 frags."
      achieved="{data($terrible-game/@achieved)}">{
      if ( $terrible-game/@achieved = "true" ) then (attribute when { /game[@id = data($terrible-game/@at-game)]/@date }) else (
      )
      }<!----></achievement-card>
    ,
    let $slaughterer := $record/achievements/slaughterer
    return <achievement-card
      type="butcher"
      achievementTitle="Butcher"
      description="Make over 80 kills in a game."
      achieved="{data($slaughterer/@achieved)}">{
      if ( $slaughterer/@achieved = "true" ) then (attribute when { /game[@id = data($slaughterer/@at-game)]/@date }) else (

      )
      }<!----></achievement-card>
    ,
    let $dday := $record/achievements/dday
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
    type="lucky-luke"
    achieved="{data($tosok-lover/@achieved)}"
    achievementTitle="Lucky Luke"
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
  {
  for $a in ($capture-achievement-bar, $simple-achievements, $progress-achievements)

  let $b :=
    if ( $a/@when )
    then (
      copy $b := $a
      modify replace value of node $b/@when with substring($a/@when, 1, 10)
      return $b
    ) else ($a)
  let $c :=
    if ( $b/@totalInLevel and $b/@progressInLevel )
    then (
      let $percentage := xs:int(100 * data($b/@progressInLevel) div data($b/@totalInLevel))
      return
        copy $c := $b
        modify insert node (attribute progressPercent { $percentage }) into $c
        return $c
    ) else ($b)
    order by $c/@achieved = "true" descending, $c/@when descending, if ( $c/@progressPercent ) then (xs:int($c/@progressPercent)) else (0) descending, not(empty($c/@progressInLevel)) descending
  return $c
  }


   </div>

return
  <div class="main-info">
    <div class="basics">
      {$basics-table}
    </div>
    <div class="achievements">
    <h3>Achievements</h3>
    {$achievements}
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

<section id="vids">
<h2>Mentions on YouTube</h2>
<p>Upload a video with a description that links to your games or profiles.<br/><i>Let us know when you upload one so we can approve it.</i></p>
{
let $vids :=
  for $approved-video in /video-approved
  let $id := data($approved-video/@id)
  for $video in /video
  where $video/@id = $id
  let $games-ids := $video//game
  where ($user-record//played-in-game/@game-id = $games-ids)
    or ($video//player = data($u/@id))
  order by $video/@published-at descending
  let $uri := "//www.youtube-nocookie.com/embed/"||$id||"?rel=0"
  let $game-ln :=
    for $game in subsequence(/game[@id = $games-ids],1,1)
    let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
    return <p class="game-ln">{local:game-header($game, $has-demo)}</p>
  return <li><iframe width="480" height="270" src="{$uri}" frameborder="0" allowfullscreen="allowfullscreen"><!----></iframe>{$game-ln}</li>
return if ( empty($vids)) then() else (<ol id="vids">{$vids}</ol>)
}
</section>
</div></profile></player>]]>
      </rest:text>
      <rest:variable name="user-id" value={userId}/>
    </rest:query>
    BasexProviderPlugin.awaitPlugin.query(theXml).map(x => Option(x).filter(_.body.nonEmpty).map(_.xml).map{

    xml =>
    UserProfile( name = (xml \\ "@name").text, profileData = (xml \\ "div").headOption.get.toString())
    })
  }

  def getGame(id: String) = BasexProviderPlugin.awaitPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
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
        |declare variable $game-id as xs:integer external;
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
        |return <game-card gameJson="{json:serialize($game-item)}">{comment { "loading..." }}</game-card>
        |
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