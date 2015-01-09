package acleague

import org.neo4j.graphdb.{DynamicRelationshipType, DynamicLabel, GraphDatabaseService}

import scala.xml.Elem

/**
 * Created by William on 09/01/2015.
 */
object GameXmlToGraph {

  def createGameFromXml(database: GraphDatabaseService)(gameXml: Elem) = {
    val gameNode = database.createNode(DynamicLabel.label("game"))
    gameNode.setProperty("id", (gameXml \ "@id").text)
    gameNode.setProperty("duration", (gameXml \ "@duration").text)
    gameNode.setProperty("map", (gameXml \ "@map").text)
    gameNode.setProperty("mode", (gameXml \ "@mode").text)
    gameNode.setProperty("state", (gameXml \ "@state").text)
    (gameXml \ "@winner").foreach(winner => gameNode.setProperty("winner", winner.text))
    (gameXml \ "@duration").map(duration => gameNode.setProperty("duration", duration.text.toInt))
    gameNode.setProperty("datetime", (gameXml \ "@date").text)
    gameNode.setProperty("date", (gameXml \ "@date").text.substring(0, 10).replaceAllLiterally("-", ""))
    for {
      team <- gameXml \ "team"
      teamNode = database.createNode(DynamicLabel.label("team"))
    } {
      teamNode.setProperty("game_id", (gameXml \ "@id").text)
      teamNode.setProperty("name", (team \ "@name").text)
      gameNode.createRelationshipTo(teamNode, DynamicRelationshipType.withName("has_team"))
      for {
        player <- team \ "player"
        playerNode = database.createNode(DynamicLabel.label("player"))
      } {
        // todo make monadic - some fields might not be there.
        playerNode.setProperty("name", (player \ "@name").text)
        (player \ "@score").map(score => playerNode.setProperty("score", score.text.toInt))
        (player \ "@flags").map(flags => playerNode.setProperty("flags", flags.text.toInt))
        playerNode.setProperty("frags", (player \ "@frags").text.toInt)
        playerNode.setProperty("team", (team \ "@name").text)
        (player \ "@deaths").map(deaths => playerNode.setProperty("deaths", deaths.text.toInt))
        teamNode.createRelationshipTo(playerNode, DynamicRelationshipType.withName("has_player"))
        gameNode.createRelationshipTo(playerNode, DynamicRelationshipType.withName("has_player"))
      }
    }
    gameNode
  }
}
