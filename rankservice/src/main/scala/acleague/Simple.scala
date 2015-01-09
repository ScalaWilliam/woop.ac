package acleague

import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.{GraphDatabaseService, DynamicRelationshipType, DynamicLabel}
import org.neo4j.rest.graphdb.RestGraphDatabase

import scala.xml.Elem

object Simple extends App {
  /** *
    *
    *

  WITH [{map: "ac_mines", mode: "ctf"}, {map: "ac_urban", mode: "ctf"}, {map: "ac_desert", mode: "team one shot, one kill"}] AS maps
  FOREACH (map in maps  | CREATE (a:map{map: map.map, mode: map.mode}));

  MATCH (g:game), (m: map) WHERE g.map = m.map AND g.mode = m.mode MERGE (g)-[hm:has_map]->(m) RETURN hm;
  MATCH (u:user), (p: player{name: u.name}) MERGE (p)-[:is_user]->(u);


    */



//  val yay = new TestGraphDatabaseFactory().newImpermanentDatabase()
  val yay = new RestGraphDatabase("http://localhost:7474/db/data")
//  val yay = new TestGraphDatabaseFactory().newImpermanentDatabase()
  val tx = yay.beginTx()
  try {
//    createGameFromXml(yay)(SampleGames.simpleGame)
//
//    for { gameXml <- (SampleGames.games \\ "game").headOption } {
//      createGameFromXml(yay)(gameXml.asInstanceOf[Elem])
//    }

//    val eng = new ExecutionEngine(yay)
//    val result = eng.execute( """MATCH (a: player)-->(c: game)<--(b:player) RETURN [a,b]""")
//    val result = eng.execute( """MATCH (a)-[b]->(c) RETURN [a,b,c]""")
//    println(result.dumpToString())
  } finally tx.close()
}