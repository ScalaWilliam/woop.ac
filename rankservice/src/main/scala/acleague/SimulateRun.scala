package acleague

import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.neo4j.cypher.ExecutionEngine
import org.neo4j.test.TestGraphDatabaseFactory

import scala.xml.Elem

object SimulateRun extends App {

  val inputQuery = <rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[
    <games>{
subsequence(
for $game in /game
let $date := xs:dateTime($game/@date) cast as xs:date
order by $date descending
return $game, 1, 1000
)}</games>
]]></rest:text>
  </rest:query>

  def createUser(name: String) = {
    ee.execute( """MERGE (user: user{ name: {name} }) RETURN user""", Map("name" -> name)).close()
  }
  def createUserPlayerLinks() = {
    ee.execute("""MATCH (u:user), (p: player{name: u.name}) MERGE (p)-[:is_user]->(u);""").close()
  }
  def updateMaps() = ee.execute("""MATCH (g:game) MERGE (m: map{name: g.map, map: g.map, mode: g.mode})""").close()
  def createMapGameLinks() = ee.execute("""MATCH (g:game), (m: map) WHERE g.map = m.map AND g.mode = m.mode MERGE (g)-[hm:on_map]->(m) RETURN hm;""").close()
  val result = Request.Post("http://odin.duel.gg:1238/rest/acleague")
    .bodyString(s"$inputQuery", ContentType.APPLICATION_XML)
    .execute().returnContent().asString()
  val startTime = System.currentTimeMillis()
  val database = new TestGraphDatabaseFactory().newImpermanentDatabase()
  val ee = new ExecutionEngine(database)
  val txn = database.beginTx()

  def executeResource(link: String)= {
    val query = scala.io.Source.fromInputStream(getClass.getResourceAsStream(link)).getLines().mkString("\n")
    val r = ee.execute(query)
    try r.toList finally r.close()
  }

  def listAchievements = {
    val eng = new ExecutionEngine(database)
    val result = eng.execute(
      """MATCH (u: user)-[link]->(achievement: fifty_flags_achievement)
        |OPTIONAL MATCH (achievement)-[:includes]->(p:player)<-[:has_player]-(g: game)
        |WITH u, achievement, link, collect(g.id) AS game_ids
        |RETURN u.name AS user, type(link) = "ongoing" AS is_ongoing, type(link) = "completed" AS is_completed, achievement.overflow AS overflow, achievement.flags AS flags, game_ids
      """.stripMargin)
    val iter = for { item <- result } yield {
        (
        item("user").toString,
        item("is_completed").toString.toBoolean,
        item("flags").toString.toInt
      )
    }
    iter.toList.toSet
  }
  val cnts = <cn><cnt name="|oNe|.eSt!gMaTa">67</cnt>
    <cnt name="[PSY]quico">60</cnt>
    <cnt name="w00p|Redbull">60</cnt>
    <cnt name="queensKing">57</cnt>
    <cnt name="~FEL~.RayDen">54</cnt>
    <cnt name="DeathCrew.45">52</cnt>
    <cnt name="w00p|Lucas">51</cnt>
    <cnt name="|AoX|Subby">50</cnt>
    <cnt name="|AoX|Grazy">49</cnt>
    <cnt name="{BoB}Jonux">47</cnt>
    <cnt name="w00p|Honorus">42</cnt>
    <cnt name="w00p|Sanzo">39</cnt>
    <cnt name="|AoX|madcatz^">37</cnt>
    <cnt name="w00p|Drakas">36</cnt>
    <cnt name="-xW-#$w@rM3D*">34</cnt>
    <cnt name="-xW-#Hustlin">34</cnt>
    <cnt name="{BoB}Narco[T]iK">33</cnt>
    <cnt name="w00p|Honor">30</cnt>
    <cnt name="vanquish">30</cnt>
    <cnt name="FD*EndGame">28</cnt>
    <cnt name="w00p|Dam.">27</cnt>
    <cnt name="w00p|Harrek">27</cnt>
    <cnt name="Reus">25</cnt>
    <cnt name="Rush=MyS=">24</cnt>
    <cnt name="Furios=MyS=">23</cnt>
    <cnt name="FD*Gazda">23</cnt>
    <cnt name="FD*Federico.">21</cnt>
    <cnt name="UlMinion(URU)">21</cnt>
    <cnt name="~FEL~Bernatix">21</cnt>
    <cnt name="-xW-#$tOuN3*">20</cnt>
    <cnt name="~FEL~SEXOLOCO">20</cnt>
    <cnt name="Frutis">20</cnt>
    <cnt name="-RT-KGB">20</cnt>
    <cnt name="-SoW-Geatzo">20</cnt>
    <cnt name="-M|A-baRute">19</cnt>
    <cnt name="~FEL~MR.JAM">19</cnt>
    <cnt name="Lozi*">19</cnt>
    <cnt name="w00p|Lipe">19</cnt>
    <cnt name="w00p|.ech0">19</cnt>
    <cnt name="FD*armagedon">18</cnt>
    <cnt name="|#LC*|LuCk">18</cnt>
    <cnt name="Omena">16</cnt>
    <cnt name="Sveark=MyS=">16</cnt>
    <cnt name="CR7">16</cnt>
    <cnt name="|#LC*|m@C">16</cnt>
    <cnt name="-Sofiane=MyS=">16</cnt>
    <cnt name="-Sofiane.">15</cnt>
    <cnt name="STK#">15</cnt>
    <cnt name="unarmed">15</cnt>
    <cnt name="w00p|">14</cnt>
    <cnt name="|oNe|OpTic">14</cnt>
    <cnt name="Hustlin">14</cnt>
    <cnt name="GeoRGeteman!!">14</cnt>
    <cnt name="|GoW|G1gantuan">14</cnt>
    <cnt name="Dora">14</cnt>
    <cnt name="tFEL~JavySoMPeR">14</cnt>
    <cnt name="Cr!sis|Grazy">13</cnt>
    <cnt name="Robtics">13</cnt>
    <cnt name="USA|LuCk">13</cnt>
    <cnt name="-SoW-GeatZo^">13</cnt>
    <cnt name="~FEL~HGF-ARG">13</cnt>
    <cnt name="DES|k4z">12</cnt>
    <cnt name="oNe|ramb0">12</cnt>
    <cnt name="Pi_1Cap">12</cnt>
    <cnt name="lozi.ann">12</cnt>
    <cnt name="Ketar*">11</cnt>
    <cnt name="{BoB}Soviet">11</cnt>
    <cnt name="Elite">11</cnt>
    <cnt name="Razor">11</cnt>
    <cnt name="LeaN~">11</cnt>
    <cnt name="AC...">11</cnt>
    <cnt name="-SoW-S.Ryan">11</cnt>
    <cnt name="-xW-#Edward">11</cnt>
    <cnt name="Friteq">11</cnt>
    <cnt name="FURY">10</cnt>
    <cnt name=".rC|x3mi">10</cnt>
    <cnt name="Maikelele">10</cnt>
    <cnt name="FrT|LuniK">10</cnt>
    <cnt name="Million">10</cnt>
  </cn>
  (cnts \\"cnt").map(_\"@name").map(_.text).foreach(createUser)
  for { game <- scala.xml.XML.loadString(result) \\ "game" }
    yield {
    GameXmlToGraph.createGameFromXml(database)(game.asInstanceOf[Elem])
    createUserPlayerLinks()
    updateMaps()
    executeResource("/fifty_games/progress.cql")
    executeResource("/fifty_games/achieved.cql")
    executeResource("/fifty_flags/progress.cql")
    executeResource("/fifty_flags/completions.cql")
  }
  def listFiftyGamesAchievements: Set[(String, Int, Int, Int, Boolean)]  = {
    val r = ee.execute(
      """
MATCH (u: user)-[link]->(ach: fifty_games)
RETURN u.name as username, ach.remain as remain, ach.target as target, ach.games as games, type(link) = "completed" as isCompleted
      """
    )
    val ok = for { i <- r
    }
    yield (
        i("username").toString,
        i("remain").toString.toInt,
        i("target").toString.toInt,
        i("games").toString.toInt,
        i("isCompleted").toString.toBoolean)
    try ok.toSet finally r.close()
  }

  println(listFiftyGamesAchievements)
  println(listAchievements)

  val endTime = System.currentTimeMillis()

  import scala.concurrent.duration._
  println(((endTime - startTime)/1000).seconds)
}