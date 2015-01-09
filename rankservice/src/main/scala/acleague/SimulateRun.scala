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
  val topUsers = Set("|oNe|.eSt!gMaTa", "[PSY]quico", "w00p|Redbull", "queensKing", "~FEL~.RayDen", "DeathCrew.45", "w00p|Lucas", "|AoX|Subby", "|AoX|Grazy", "{BoB}Jonux", "w00p|Honorus", "w00p|Sanzo", "|AoX|madcatz^", "w00p|Drakas", "-xW-#$w@rM3D*", "-xW-#Hustlin")
  topUsers foreach createUser
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

}