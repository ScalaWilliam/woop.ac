package acleague.achievements

import acleague.GameXmlToGraph
import org.neo4j.cypher.ExecutionEngine
import org.scalatest.{Inspectors, Matchers, WordSpec}
class FiftyTest extends WordSpec with Matchers with Inspectors with Neo4jTester {
  case class Achievement(name: String, ongoing: Boolean, completed: Boolean, flags:Int, overflow: Option[Int], games: Set[String])
  def executeProgress(): Unit = executeResource("/fifty_flags/progress.cql")
  def executeCompletion(): Unit = executeResource("/fifty_flags/completions.cql")
  /** Bloody hell, mate... **/
  "Fifty Test" must {

    def listAchievements = {
      val eng = new ExecutionEngine(database)
      val result = eng.execute(
        """MATCH (u: user)-[link]->(achievement: fifty_flags_achievement)
          |OPTIONAL MATCH (achievement)-[:includes]->(p:player)<-[:has_player]-(g: game)
          |WITH u, achievement, link, collect(g.id) AS game_ids
          |RETURN u.name AS user, type(link) = "ongoing" AS is_ongoing, type(link) = "completed" AS is_completed, achievement.overflow AS overflow, achievement.flags AS flags, game_ids
        """.stripMargin)
      val iter = for { item <- result } yield {
        Achievement(
          name = item("user").toString,
          ongoing = item("is_ongoing").toString.toBoolean,
          completed = item("is_completed").toString.toBoolean,
          flags = item("flags").toString.toInt,
          overflow = Option(item("overflow")).map(_.toString.toInt),
          games = item("game_ids").asInstanceOf[List[Any]].map(_.toString).toSet
        )
      }
      iter.toList.toSet
    }


    "List nothing for no users" in {
      listAchievements shouldBe empty
    }

    "Create blanks for users" in {
      // expect achievements: Drakas = ongoing(0)->[], Sanzo = ongoing(0)->[]
      createUser("Drakas")
      createUser("Sanzo")
      executeProgress()
      listAchievements should have size 2
      forExactly(1, listAchievements) (_.name shouldBe "Drakas")
      forExactly(1, listAchievements) (_.name shouldBe "Sanzo")
      forAll(listAchievements) {
        case Achievement(name, ongoing, completed, flags, overflow, games) =>
          ongoing shouldBe true
          flags shouldBe 0
          overflow shouldBe None
          completed shouldBe false
          games shouldBe empty
      }
    }

    "Not change anything after an extra runs" in {
      val first = listAchievements
      executeProgress()
      val second = listAchievements
      first shouldBe second
    }

    "Include basic data" in {
      val firstGame = <game id="822779981" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="Drakas" host="80.47.111.152" score="1029" flags="5" frags="58" deaths="33"/>
        </team>
        <team name="CLA" flags="5" frags="128">
          <player name="Sanzo" host="145.118.113.26" score="610" flags="3" frags="41" deaths="56"/>
        </team>
      </game>
      GameXmlToGraph.createGameFromXml(database)(firstGame)
      createUserPlayerLinks()
      val first = listAchievements
      executeProgress()
      executeCompletion()
      val second = listAchievements
      //println(ee.execute("""MATCH (n)-[:includes]->(m) RETURN n,m;""").dumpToString())
      first should not be second
      second should have size 2

      forAll(listAchievements) {
        case Achievement(name, ongoing, completed, flags, overflow, games) =>
          ongoing shouldBe true
          overflow shouldBe None
          completed shouldBe false
          games shouldBe Set("822779981")
      }

      forExactly(1, listAchievements) {
        case Achievement(name, _, _, flags, _, _) =>
          name shouldBe "Sanzo"
          flags shouldBe 3
      }
      forExactly(1, listAchievements) {
        case Achievement(name, _, _, flags, _, _) =>
          name shouldBe "Drakas"
          flags shouldBe 5
      }

    }
    "Running it again should change nothing" in {
      val first = listAchievements
      executeProgress()
      val second = listAchievements
      first shouldBe second
    }
    "Perform the sequence correctly" in {
      deleteEverything()
      executeProgress()
      listAchievements shouldBe empty

      createUser("Drakas")
      executeProgress()
      executeCompletion()
      listAchievements should have size 1
      def game(id: String, playerName: String, flags: Int) =
        <game id={id} duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF"><team name="RVSF" flags="7" frags="147">
          <player name={playerName} host="80.47.111.152" score="1029" flags={flags.toString} frags="58" deaths="33"/></team></game>

      GameXmlToGraph.createGameFromXml(database)(game(id = "G1", playerName = "Drakas", flags = 20))
      createUserPlayerLinks()
      executeProgress()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = true, completed = false, 20, overflow = None, Set("G1"))
      )
      executeCompletion()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = true, completed = false, 20, overflow = None, Set("G1"))
      )

      GameXmlToGraph.createGameFromXml(database)(game(id = "G2", playerName = "Drakas", flags = 40))
      createUserPlayerLinks()
      executeProgress()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = true, completed = false, 50, overflow = Some(10), Set("G1", "G2"))
      )
      executeCompletion()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = Some(10), Set("G1", "G2")),
        Achievement("Drakas", ongoing = true, completed = false, 10, overflow = None, Set())
      )

      GameXmlToGraph.createGameFromXml(database)(game(id = "G3", playerName = "Drakas", flags = 40))
      createUserPlayerLinks()
      executeProgress()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = Some(10), Set("G1", "G2")),
        Achievement("Drakas", ongoing = true, completed = false, 50, overflow = None, Set("G3"))
      )
      executeCompletion()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = None, Set("G1", "G2")),
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = None, Set("G3")),
        Achievement("Drakas", ongoing = true, completed = false, 0, overflow = None, Set())
      )
      executeProgress()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = None, Set("G1", "G2")),
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = None, Set("G3")),
        Achievement("Drakas", ongoing = true, completed = false, 0, overflow = None, Set())
      )
      executeCompletion()
      listAchievements shouldBe Set(
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = None, Set("G1", "G2")),
        Achievement("Drakas", ongoing = false, completed = true, 50, overflow = None, Set("G3")),
        Achievement("Drakas", ongoing = true, completed = false, 0, overflow = None, Set())
      )

    }
    "Perform correctly" in {
      // todo not sure how to record 'dependent' games. Not sure we really can be bothered, frankly.
      // todo we'll just know it's inaccurate. Meh :D



      // register three users
      // add the following games:
      //     A  B  C   <- flags
      // 1  20 20  -
      // expect achievements: A = ongoing(20)->[1], B = ongoing(20)->[1], C = ongoing(0)->[]
      // 2  20  - 20
      // expect achievements: A = ongoing(40)->[1,2], B = ongoing(20)->[1], C = ongoing(20)->[2]
      // 3  20  -  -
      // expect achievements: A = completed(50)->[1,2,3], A = ongoing(10)->[], B = ongoing(20)->[1], C = ongoing(20)->[2]
      // 4  15 55 20
      // expect achievements: A = completed(50)->[1,2,3], A = ongoing(25)->[4], B = completed(50)->[1,4], B = ongoing(25)->[], C = ongoing(40)->[2,4]
      // 5  15 7  14
      // expect achievements: A = completed(50)->[1,2,3], A = ongoing(40)->[4,5], B = completed(50)->[1,4], B = ongoing(32)->[5], C = completed(50)->[2,4,5], C = ongoing(4)->[5]
      // expect another run to not change anything

      deleteEverything()
      executeProgress()
      listAchievements shouldBe empty

      createUser("A")
      createUser("B")
      createUser("C")
      executeProgress()
      executeCompletion()
      listAchievements should have size 3

      val firstGame = <game id="G1" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="A" host="80.47.111.152" score="1029" flags="20" frags="58" deaths="33"/>
        </team>
        <team name="CLA" flags="5" frags="128">
          <player name="B" host="145.118.113.26" score="610" flags="20" frags="41" deaths="56"/>
        </team>
      </game>

      GameXmlToGraph.createGameFromXml(database)(firstGame)
      createUserPlayerLinks()
      executeProgress()
      executeCompletion()
      listAchievements shouldBe Set(
        Achievement("A", ongoing = true, completed = false, 20, overflow = None, Set("G1")),
        Achievement("B", ongoing = true, completed = false, 20, overflow = None, Set("G1")),
        Achievement("C", ongoing = true, completed = false, 0, overflow = None, Set())
      )

      val secondGame = <game id="G2" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="A" host="80.47.111.152" score="1029" flags="21" frags="58" deaths="33"/>
        </team>
        <team name="CLA" flags="5" frags="128">
          <player name="C" host="145.118.113.26" score="610" flags="20" frags="41" deaths="56"/>
        </team>
      </game>

      GameXmlToGraph.createGameFromXml(database)(secondGame)
      createUserPlayerLinks()
      executeProgress()
      executeCompletion()
      // expect achievements: A = ongoing(40)->[1,2], B = ongoing(20)->[1], C = ongoing(20)->[2]
      listAchievements shouldBe Set(
        Achievement("A", ongoing = true, completed = false, 41, overflow = None, Set("G1", "G2")),
        Achievement("B", ongoing = true, completed = false, 20, overflow = None, Set("G1")),
        Achievement("C", ongoing = true, completed = false, 20, overflow = None, Set("G2"))
      )

      val thirdGame = <game id="G3" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="A" host="80.47.111.152" score="1029" flags="20" frags="58" deaths="33"/>
        </team>
        <team name="CLA" flags="5" frags="128">
        </team>
      </game>

      GameXmlToGraph.createGameFromXml(database)(thirdGame)
      createUserPlayerLinks()
      executeProgress()
      listAchievements shouldBe Set(
        Achievement("A", ongoing = true, completed = false, 50, overflow = Some(11), Set("G1", "G2", "G3")),
        Achievement("B", ongoing = true, completed = false, 20, overflow = None, Set("G1")),
        Achievement("C", ongoing = true, completed = false, 20, overflow = None, Set("G2"))
      )
      executeCompletion()

      // expect achievements: A = completed(50)->[1,2,3], A = ongoing(10)->[], B = ongoing(20)->[1], C = ongoing(20)->[2]

      listAchievements shouldBe Set(
        Achievement("A", ongoing = false, completed = true, 50, overflow = Some(11), Set("G1", "G2", "G3")),
        Achievement("A", ongoing = true, completed = false, 11, overflow = None, Set()),
        Achievement("B", ongoing = true, completed = false, 20, overflow = None, Set("G1")),
        Achievement("C", ongoing = true, completed = false, 20, overflow = None, Set("G2"))
      )
      val fourthGame = <game id="G4" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="A" host="80.47.111.152" score="1029" flags="15" frags="58" deaths="33"/>
          <player name="B" host="80.47.111.152" score="1029" flags="55" frags="58" deaths="33"/>
        </team>
        <team name="CLA" flags="5" frags="128">
          <player name="C" host="145.118.113.26" score="610" flags="20" frags="41" deaths="56"/>
        </team>
      </game>

      GameXmlToGraph.createGameFromXml(database)(fourthGame)
      createUserPlayerLinks()
      executeProgress()
      listAchievements shouldBe Set(
        Achievement("A", ongoing = false, completed = true, 50, overflow = Some(11), Set("G1", "G2", "G3")),
        Achievement("A", ongoing = true, completed = false, 26, overflow = None, Set("G4")),
        Achievement("B", ongoing = true, completed = false, 50, overflow = Some(25), Set("G1", "G4")),
        Achievement("C", ongoing = true, completed = false, 40, overflow = None, Set("G2", "G4"))
      )
      executeCompletion()
      // expect achievements: A = completed(50)->[1,2,3], A = ongoing(25)->[4], B = completed(50)->[1,4], B = ongoing(25)->[], C = ongoing(40)->[2,4]

      listAchievements shouldBe Set(
        Achievement("A", ongoing = false, completed = true, 50, overflow = Some(11), Set("G1", "G2", "G3")),
        Achievement("A", ongoing = true, completed = false, 26, overflow = None, Set("G4")),
        Achievement("B", ongoing = false, completed = true, 50, overflow = Some(25), Set("G1", "G4")),
        Achievement("B", ongoing = true, completed = false, 25, overflow = None, Set()),
        Achievement("C", ongoing = true, completed = false, 40, overflow = None, Set("G2", "G4"))
      )

      val fifthGame = <game id="G5" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="A" host="80.47.111.152" score="1029" flags="15" frags="58" deaths="33"/>
        </team>
        <team name="CLA" flags="5" frags="128">
          <player name="B" host="80.47.111.152" score="1029" flags="7" frags="58" deaths="33"/>
          <player name="C" host="145.118.113.26" score="610" flags="14" frags="41" deaths="56"/>
        </team>
      </game>

      GameXmlToGraph.createGameFromXml(database)(fifthGame)
      createUserPlayerLinks()
      executeProgress()
      executeCompletion()
      // expect achievements: A = completed(50)->[1,2,3], A = ongoing(40)->[4,5], B = completed(50)->[1,4], B = ongoing(32)->[5], C = completed(50)->[2,4,5], C = ongoing(4)->[5]

      listAchievements shouldBe Set(
        Achievement("A", ongoing = false, completed = true, 50, overflow = Some(11), Set("G1", "G2", "G3")),
        Achievement("A", ongoing = true, completed = false, 41, overflow = None, Set("G4", "G5")),
        Achievement("B", ongoing = false, completed = true, 50, overflow = Some(25), Set("G1", "G4")),
        Achievement("B", ongoing = true, completed = false, 32, overflow = None, Set("G5")),
        Achievement("C", ongoing = false, completed = true, 50, overflow = Some(4), Set("G2", "G4", "G5")),
        Achievement("C", ongoing = true, completed = false, 4, overflow = None, Set())
      )

      val finalAchievementsA = listAchievements
      executeProgress()
      val finalAchievementsB = listAchievements
      finalAchievementsB shouldBe finalAchievementsA
      executeCompletion()
      val finalAchievementsC = listAchievements
      finalAchievementsB shouldBe finalAchievementsC

      val siixthTest = <game id="G5" duration="15" date="2015-01-08T22:25:37Z" server="62-210-131-155.rev.poneytelecom.eu aura AssaultCube[local#2999]" map="ac_urban" mode="ctf" state="match" winner="RVSF">
        <team name="RVSF" flags="7" frags="147">
          <player name="Q" host="80.47.111.152" score="1029" flags="15" frags="58" deaths="33"/>
        </team>
        <team name="CLA" flags="5" frags="128">
          <player name="W" host="80.47.111.152" score="1029" flags="7" frags="58" deaths="33"/>
          <player name="Z" host="145.118.113.26" score="610" flags="14" frags="41" deaths="56"/>
        </team>
      </game>
      // no players here, this should fial
      GameXmlToGraph.createGameFromXml(database)(siixthTest)
      createUserPlayerLinks()
      executeProgress()
      executeCompletion()


      val finalAchievementsD = listAchievements
      finalAchievementsC shouldBe finalAchievementsD
    }
  }
}