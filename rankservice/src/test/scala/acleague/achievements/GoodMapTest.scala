package acleague.achievements

import acleague.GameXmlToGraph
import org.scalatest.{Matchers, WordSpec}

/**
 * Created by William on 09/01/2015.
 */
class GoodMapTest extends WordSpec with Matchers with Neo4jTester{
  def executeProgress() = executeResource("/good_map/progress.cql")
  def executeCompletion() = executeResource("/good_map/achieved.cql")

  def listMapAchievements: Set[(String, String, String, Boolean, Int, Int)]  = {
    val r = ee.execute(
      """
MATCH (u: user)-[link]->(ach: map_completed_achievement)-[:of_map]->(m: good_map)
RETURN u.name as username, m.map As mapname, m.mode as mapmode, type(link) = "completed" as isCompleted, ach.rvsfRemain as rvsfGamesLeft, ach.claRemain as claGamesLeft
        """
    )
    val ok = for { i <- r
    }
    yield (
        i("username").toString,
        i("mapname").toString,
        i("mapmode").toString,
        i("isCompleted").toString.toBoolean,
        i("rvsfGamesLeft").toString.toInt,
        i("claGamesLeft").toString.toInt)
    try ok.toSet finally r.close()
  }

  "Good map test" must {

    "Give nothing" in {
      listMapAchievements shouldBe empty
    }
    "Create no definitions" in {
      deleteEverything()
      createUser("Johnny")
      listMapAchievements shouldBe empty
      executeProgress()
      executeCompletion()
      listMapAchievements shouldBe empty
    }

    "Create one definition only" in {
      addMap("ac_mines", "ctf")
      createUser("Johnny")
      listMapAchievements shouldBe empty
      executeProgress()
      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 2, 2))
      executeCompletion()
      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 2, 2))
      executeProgress()
      executeCompletion()
      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 2, 2))
    }

    "Maps don't mix" in {
      deleteEverything()
      addMap("ac_mines", "ctf")
      addMap("ac_elevation", "ctf")
      createUser("Johnny")
      GameXmlToGraph.createGameFromXml(database)(<game id="A" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_mines" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="Johnny" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
      </game>)
      createUserPlayerLinks()
      createMapGameLinks()
      executeProgress()
      executeCompletion()
      GameXmlToGraph.createGameFromXml(database)(<game id="B" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_elevation" mode="ctf" state="match" winner="RVSF">
        <team name="CLA" flags="7" frags="147">
          <player name="Johnny" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
      </game>)
      createUserPlayerLinks()
      createMapGameLinks()
      executeProgress()
      executeCompletion()
      listMapAchievements shouldBe Set(("Johnny","ac_mines","ctf",false,1,2), ("Johnny","ac_elevation","ctf",false,2,1))
      deleteEverything()
      addMap("ac_mines", "ctf")
      createUser("Johnny")
    }

    "Ignore an unknown map" in {
      GameXmlToGraph.createGameFromXml(database)(<game id="A" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_exy" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="Johnny" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
      </game>)
      createUserPlayerLinks()
      createMapGameLinks()
      executeProgress()
      executeCompletion()
      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 2, 2))
    }
    "Accept a known map" in {
      GameXmlToGraph.createGameFromXml(database)(<game id="B" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_mines" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="Johnny" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
      </game>)
      createUserPlayerLinks()
      createMapGameLinks()

      executeProgress()
      executeCompletion()
      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 1, 2))
    }
    "Finish player off" in {
      GameXmlToGraph.createGameFromXml(database)(<game id="C" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_mines" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="Johnny" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
      </game>)
      createUserPlayerLinks()
      createMapGameLinks()

      executeProgress()
      executeCompletion()

      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 0, 2))

      GameXmlToGraph.createGameFromXml(database)(<game id="D" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_mines" mode="ctf" state="match" winner="RVSF">
        <team name="CLA" flags="7" frags="147">
          <player name="Johnny" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
      </game>)
      createUserPlayerLinks()
      createMapGameLinks()

      executeProgress()
      executeCompletion()

      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 0, 1))
      GameXmlToGraph.createGameFromXml(database)(<game id="E" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_mines" mode="ctf" state="match" winner="RVSF">
        <team name="CLA" flags="7" frags="147">
          <player name="Johnny" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
      </game>)
      createUserPlayerLinks()
      createMapGameLinks()

      executeProgress()
      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", false, 0, 0))
      executeCompletion()
      listMapAchievements shouldBe Set(("Johnny", "ac_mines", "ctf", true, 0, 0))
    }

  }
}
