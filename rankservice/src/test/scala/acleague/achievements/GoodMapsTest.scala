package acleague.achievements

import org.scalatest.{Matchers, WordSpec}

/**
 * Created by William on 09/01/2015.
 */
class GoodMapsTest extends WordSpec with Matchers with Neo4jTester {

  def executeProgress() = executeResource("/good_maps/progress.cql")
  def executeCompletion() = executeResource("/good_maps/achieved.cql")

  def listMapsAchievements: Set[(String, Int, Int, Int, Boolean)]  = {
    val r = ee.execute(
      """
MATCH (u: user)-[link]->(ach: maps_completed_achievement)
RETURN u.name as username, ach.remain as remain, ach.target as target, ach.progress as progress, type(link) = "completed" as isCompleted
      """
    )
    val ok = for { i <- r
    }
    yield (
        i("username").toString,
        i("remain").toString.toInt,
        i("target").toString.toInt,
        i("progress").toString.toInt,
        i("isCompleted").toString.toBoolean)
    try ok.toSet finally r.close()
  }
  "Good maps" must {
  "Return nothing for nothing" in {
    executeProgress()
    executeCompletion()
    listMapsAchievements shouldBe empty
  }
    "Create a blank achievement for a new person" in {
      createUser("Newbie")
      addMap("ac_mines", "ctf")
      addMap("ac_elevation", "ctf")
      executeProgress()
      executeCompletion()
      listMapsAchievements shouldBe empty
      ee.execute(
        """MATCH (u:user{name: "Newbie"}), (m:map{name:"ac_mines"}), (m2: map{name:"ac_elevation"})
          |MERGE (u)-[:ongoing]->(:map_completed_achievement)-[:of_map]->(m)
          |MERGE (u)-[:ongoing]->(:map_completed_achievement)-[:of_map]->(m2)
          |""".stripMargin
      )
      executeProgress()
      executeCompletion()

      println(ee.execute(
        """MATCH (a)-[:depends_on]->(b)-[:of_map]->(m)
          |RETURN a,b,m;
          |""".stripMargin
      ).dumpToString())
      listMapsAchievements shouldBe Set(("Newbie", 2, 2, 0, false))

      ee.execute(
        """MATCH (u:user{name: "Newbie"}), (m: map{name:"ac_mines"})
          |MATCH (u)-[link]->(ach:map_completed_achievement)-[:of_map]->(m)
          |DELETE link
          |MERGE (u)-[:completed]->(ach)
          |""".stripMargin
      )
      executeProgress()
      executeCompletion()
      listMapsAchievements shouldBe Set(("Newbie", 1, 2, 1, false))

      ee.execute(
        """MATCH (u:user{name: "Newbie"}), (m: map{name:"ac_elevation"})
          |MATCH (u)-[link]->(ach:map_completed_achievement)-[:of_map]->(m)
          |DELETE link
          |MERGE (u)-[:completed]->(ach)
          |""".stripMargin
      )
      executeProgress()
      executeCompletion()
      listMapsAchievements shouldBe Set(("Newbie", 0, 2, 2, true))

    }
  "Work properly" in {

  }
}

}
