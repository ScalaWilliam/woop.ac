package acleague.achievements

import org.neo4j.cypher.ExecutionEngine
import org.neo4j.test.TestGraphDatabaseFactory

/**
 * Created by William on 09/01/2015.
 */
trait Neo4jTester {
  def createMapGameLinks() = ee.execute("""MATCH (g:game), (m: map) WHERE g.map = m.map AND g.mode = m.mode MERGE (g)-[hm:on_map]->(m) RETURN hm;""").close()

  def addMap(name: String, mode: String) = {
    ee.execute( """MERGE (good_map: good_map:map{ name: {name}, map: {name}, mode: {mode} }) RETURN good_map""", Map("mode" -> mode, "name" -> name)).close()
  }
  lazy val (database, txn, ee) = {
    val db = new TestGraphDatabaseFactory().newImpermanentDatabase()
    val txn = db.beginTx()
    (db, txn, new ExecutionEngine(db))
  }

  def deleteEverything(): Unit = {
    ee.execute("""MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r""").close()
  }
  def createUser(name: String) = {
    ee.execute( """MERGE (user: user{ name: {name} }) RETURN user""", Map("name" -> name)).close()
  }
  def createUserPlayerLinks() = {
    ee.execute("""MATCH (u:user), (p: player{name: u.name}) MERGE (p)-[:is_user]->(u);""").close()
  }
  def executeResource(link: String)= {
    val query = scala.io.Source.fromInputStream(getClass.getResourceAsStream(link)).getLines().mkString("\n")
    val r = ee.execute(query)
    try r.toList finally r.close()
  }

}
