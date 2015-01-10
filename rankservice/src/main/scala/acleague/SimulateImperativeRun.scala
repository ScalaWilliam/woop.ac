package acleague

import acleague.Imperative.{Player, User}
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

  val userRepository = scala.collection.mutable.HashMap.empty[String, User[String]]

  def userLookup(player:Player): Option[User[String]] = {
    userRepository.get(player.name)
  }

  for {
    nameNode <- cntsXml \\ "@name"
    name = nameNode.text
  } userRepository += name -> new User[String](id = name)

  val results = for {
    gameElem <- gamesXml \\ "game"
    game = Imperative.createGameFromXml(gameElem.asInstanceOf[Elem])
  } yield Imperative.acceptGame(userLookup)(game)

  userRepository foreach println
  results.flatten foreach println

  userRepository map(_._2.toXml) foreach println

  val wat = for {
    (_, user) <- userRepository
    xmlDoc = user.toXml
  }yield {
    Request.Post("http://odin.duel.gg:1238/rest/acleague")
      .bodyString(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[
      for $user-record in /user-record
      let $id := data($user-record/@id)
      let $existing-record := db:open("acleague")/user-record[@id=$id]
      return if ( empty($existing-record) ) then (db:add("acleague", $user-record, "manual-user-record"))
      else (replace node $existing-record with $user-record)
]]></rest:text>
      <rest:context>{xmlDoc}</rest:context>
    </rest:query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString()
  }
  wat foreach println

}