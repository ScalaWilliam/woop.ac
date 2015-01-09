package acleague.achievements

import acleague.GameXmlToGraph
import org.scalatest.{Matchers, WordSpec}

/**
 * Created by William on 09/01/2015.
 */
class FiftyGamesTest extends WordSpec with Matchers with Neo4jTester {

  def executeProgress() = executeResource("/fifty_games/progress.cql")
  def executeCompletion() = executeResource("/fifty_games/achieved.cql")

  def createGame(name: String, id: String): Unit = {
    GameXmlToGraph.createGameFromXml(database)(<game id={id} duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_exy" mode="ctf" state="match" winner="RVSF">
      <team name="RVSF" flags="7" frags="147">
        <player name={name} host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
      </team>
    </game>)
    createUserPlayerLinks()
    createMapGameLinks()
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
  "Yuppy" must {
    "List nothing" in {
      executeProgress()
      executeCompletion()
      listFiftyGamesAchievements shouldBe empty
    }

    "Work just fine" in {
      createUser("Johnny")
      createGame("Johnny", "abc")
      executeProgress()
      executeCompletion()
      listFiftyGamesAchievements should not be empty
      listFiftyGamesAchievements shouldBe Set(("Johnny", 49, 50,1, false))
    }
    "Increment by many games" in {
      for { i <- 1 to 50 } {
        createGame("Johnny", s"B$i")
        executeProgress()
        executeCompletion()
      }
      listFiftyGamesAchievements shouldBe Set(("Johnny", 49, 50, 1, false), ("Johnny", 0, 50, 50, true))
    }
  }
}
