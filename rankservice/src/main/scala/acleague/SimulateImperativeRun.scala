package acleague

import acleague.Imperative.User
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType

import scala.xml.Elem

object SimulateImperativeRun extends App {

  val gamesXml = scala.xml.XML.loadString(Request.Post("http://odin.duel.gg:1238/rest/acleague")
    .bodyString(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[
    <games>{
subsequence(
for $game in /game
let $date := xs:dateTime($game/@date) cast as xs:date
order by $date ascending
return $game, 1, 22200
)}</games>
]]></rest:text>
  </rest:query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString())

  val cntsXml = scala.xml.XML.loadString(Request.Post("http://odin.duel.gg:1238/rest/acleague")
    .bodyString(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[<cnts>{
for $player in //player
let $name := data($player/@name)
group by $name
let $cnt := count($player)
where $cnt ge 20
order by $cnt descending
return <cnt name="{$name}">{$cnt}</cnt>}</cnts>
]]></rest:text>
  </rest:query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString())

  val userRepository = scala.collection.mutable.HashMap.empty[String, User]

  for {
    nameNode <- cntsXml \\ "@name"
    name = nameNode.text
  } userRepository += name -> new User(id = name)

  for {
    gameElem <- gamesXml \\ "game"
    game = Imperative.createGameFromXml(gameElem.asInstanceOf[Elem])
  } yield Imperative.acceptGame(userRepository)(game)
  userRepository foreach println

}