package acleague
import acleague.ingesters._
import org.scalatest._

class ParserSpec extends WordSpec with Inside with Inspectors with Matchers with OptionValues {
  "Demo capture" must {
    "Not fail another demo" in {
      val inputSequence =
        """
          |Demo "Thu Dec 18 19:24:56 2014: ctf, ac_gothic, 610.60kB" recorded.
          |demo written to file "demos/20141218_1824_local_ac_gothic_15min_CTF.dmo" (625252 bytes)
          |
        """.stripMargin.split("\r?\n")

      val outputs = inputSequence.scanLeft(NoDemosCollected: DemoCollector)(_.next(_))

      forExactly(1, outputs) {
        output =>
          output shouldBe a[DemoWrittenCollected]
      }
    }
    "Not fail a simple demo (1.2)" in {
      val inputSequence =
        """
          |
          |Demo "Sun Dec 14 16:47:14 2014: team deathmatch, ac_aqueous, 113.03kB, 11 mr" recorded.
          |demo written to file "demos/20141214_1547_local_ac_aqueous_4min_TDM.dmo" (115739 bytes)
          |Map height density information for ac_aqueous: H = 17.44 V = 865894, A = 49648 and MA = 2548
          |
          |Game start: team deathmatch on ac_aqueous, 6 players, 15 minutes, mastermode 0, (map rev 22/19068, off
          |
        """.stripMargin.split("\r?\n")

      val outputs = inputSequence.scanLeft(NoDemosCollected: DemoCollector)(_.next(_))

      forExactly(1, outputs) {
        output =>
          output shouldBe a [DemoWrittenCollected]
          inside(output) {
            case DemoWrittenCollected(DemoRecorded(dateTime, mode, map, sizeRecorded), DemoWritten(filename, sizeWritten)) =>
              dateTime shouldBe "Sun Dec 14 16:47:14 2014"
              mode shouldBe "team deathmatch"
              map shouldBe "ac_aqueous"
              sizeRecorded shouldBe "113.03kB"
              filename shouldBe "demos/20141214_1547_local_ac_aqueous_4min_TDM.dmo"
              sizeWritten shouldBe "115739 bytes"
          }
      }

    }
  }
  "Duration calculator" must {
    "Report current game properly" in {
      val inputSequence =
        """
          |Game status: team deathmatch on ac_aqueous, game finished, open, 6 clients
          |Game status: ctf on ac_gothic, 10 minutes remaining, open, 4 clients
          |Game status: hunt the flag on ac_depot, 15 minutes remaining, open, 4 clients
          |Game status: hunt the flag on ac_depot, 14 minutes remaining, open, 4 clients
          |Game status: team deathmatch on ac_aqueous, game finished, open, 6 clients
        """.stripMargin.split("\r?\n")
      println(inputSequence.size)
      val outputSequence = inputSequence.scanLeft(NoDurationFound: GameDuration)(_.next(_)).toList
      val List(_, _, first, second, third, fourth, fifth, _) = outputSequence
      first shouldBe NoDurationFound
      second shouldBe GameInProgress(10, 10)
      third shouldBe GameInProgress(15, 15)
      fourth shouldBe GameInProgress(15, 14)
      fifth shouldBe GameFinished(15)
    }
  }
  "Game capture" must {
    "Not fail for a TDM game" in {
      val inputSequence =
        """
          |Status at 12-12-2014 20:41:07: 1 remote clients, 0.0 send, 0.3 rec (K/sec)
          |[90.208.48.65] |oNe|OpTic says: 'i dont want to play 2v2'
          |
          |Game status: team deathmatch on ac_aqueous, game finished, open, 6 clients
          |cn name             team  score frag death tk ping role    host
          | 0 Daimon           RVSF    -12    0     3  0   32 normal  2.12.186.32
          | 1 ~FEL~.RayDen     RVSF     57    8     2  0  169 normal  186.83.65.12
          | 2 |oNe|OpTic       CLA      -4    0     1  0   42 normal  90.208.48.65
          | 3 inter            CLA      33    7     3  1   49 normal  79.169.140.46
          | 4 hu3              RVSF     -8    0     2  0  217 normal  177.83.184.175
          | 5 |AoX|Subby       SPEC      0    0     0  0   26 normal  86.92.93.88
          |   ~FEL~MR.JAM      RVSF    1    7       -    - disconnected
          |   ~FEL~MR.JAE      SPEC    1    7       -    - disconnected
          |Team  CLA:  2 players,    7 frags
          |Team RVSF:  3 players,    8 frags
          |
          |Demo "Sun Dec 14 16:47:14 2014: team deathmatch, ac_aqueous, 113.03kB, 11 mr" recorded.
          |demo written to file "demos/20141214_1547_local_ac_aqueous_4min_TDM.dmo" (115739 bytes)
          |Map height density information for ac_aqueous: H = 17.44 V = 865894, A = 49648 and MA = 2548
          |
          |Game start: team deathmatch on ac_aqueous, 6 players, 15 minutes, mastermode 0, (map rev 22/19068, off
          |
        """.stripMargin.split("\r?\n")

      val playersShouldBe = Set("Daimon", "~FEL~.RayDen", "|oNe|OpTic", "inter", "hu3", "~FEL~MR.JAM")

      val outputs = inputSequence.scanLeft(NothingFound: ParserState)(_.next(_))

      outputs foreach println
      val foundGame = outputs.find(_.isInstanceOf[FoundGame]).value

      inside(foundGame) {
        case FoundGame(header, Right(fragGame)) =>
          inside(header) {
            case GameFinishedHeader(mode, map, state) =>
              mode shouldBe GameMode.TDM
              map shouldBe "ac_aqueous"
              state shouldBe "open"
          }
          inside(fragGame) {
            case FragGameBuilder(_, scores, disconnectedScores, teamScores) =>
              teamScores should have size 2
              scores should have size 5
              disconnectedScores should have size 1

              forExactly(1, disconnectedScores) {
                score => inside(score) {
                  case TeamModes.FragStyle.IndividualScoreDisconnected(name, team, frag, death) =>
                    name shouldBe "~FEL~MR.JAM"
                    team shouldBe "RVSF"
                    frag shouldBe 1
                    death shouldBe 7
                }
              }
              forExactly(1, teamScores) {
                score => inside(score) {
                  case TeamModes.FragStyle.TeamScore(name, players, frags) =>
                    name shouldBe "RVSF"
                    players shouldBe 3
                    frags shouldBe 8
                }
              }
              forExactly(1, teamScores) {
                score => inside(score) {
                  case TeamModes.FragStyle.TeamScore(name, players, frags) =>
                    name shouldBe "CLA"
                    players shouldBe 2
                    frags shouldBe 7
                }
              }

              forExactly(1, scores) {
                pscore => inside(pscore) {
                  case TeamModes.FragStyle.IndividualScore(cn, name, team, score, frag, death, tk, ping, role, host) =>
                    cn shouldBe 3
                    name shouldBe "inter"
                    team shouldBe "CLA"
                    score shouldBe 33
                    frag shouldBe 7
                    death shouldBe 3
                    tk shouldBe 1
                    ping shouldBe 49
                    role shouldBe "normal"
                    host shouldBe "79.169.140.46"
                }
              }
          }
      }


    }
    "Not fail for an HTF game" in {
      val inputSequence = """
                            |Status at 12-12-2014 20:41:07: 1 remote clients, 0.0 send, 0.3 rec (K/sec)
                            |
                            |Game status: hunt the flag on ac_depot, game finished, open, 2 clients
                            |cn name             team flag score frag death tk ping role    host
                            | 0 Drakas           RVSF    0   9  0     0  0   12 normal  127.0.0.1
                            | 5 Srakas           SPEC    0   9  0     0  0   12 normal  127.0.0.1
                            |   w00p|Sanzo       RVSF    8   33    35  -    - disconnected
                            |   w00p|Sanzr       SPEC    8   33    35  -    - disconnected
                            |Team  CLA:  0 players,    0 frags,    0 flags
                            |Team RVSF:  1 players,    0 frags,    0 flags
                            |
                            |x
                            |""".stripMargin.split("\r?\n")

      for { t <- inputSequence } info(s"Line: $t")
      val outputs = inputSequence.scanLeft(NothingFound: ParserState)(_.next(_))

      for { o <- outputs } info(s"Output item: $o")

      inside(outputs(outputs.size-2)) {
        case FoundGame(header, Left(flagGame)) =>
          inside(header) {
            case GameFinishedHeader(mode, map, state) =>
              mode shouldBe GameMode.HTF
              map shouldBe "ac_depot"
              state shouldBe "open"
          }
          inside(flagGame) {
            case FlagGameBuilder(_, scores, disconnectedScores, teamScores) =>
              teamScores should have size 2
              scores should have size 1
              forExactly(1, disconnectedScores) {
                disconnectedScore => inside(disconnectedScore) {
                  case TeamModes.FlagStyle.IndividualScoreDisconnected(name, team, flag, score, frag) =>
                    name shouldBe "w00p|Sanzo"
                    team shouldBe "RVSF"
                    flag shouldBe 8
                    score shouldBe 33
                    frag shouldBe 35
                }
              }
              forExactly(1, teamScores) {
                score => inside(score) {
                  case TeamModes.FlagStyle.TeamScore(name, players, frags, flags) =>
                    name shouldBe "RVSF"
                    players shouldBe 1
                    frags shouldBe 0
                    flags shouldBe 0
                }
              }

              forExactly(1, scores) {
                score => inside(score) {
                  case TeamModes.FlagStyle.IndividualScore(cn, name, team, flag, score, frag, death, tk, ping, role, host) =>
                    cn shouldBe 0
                    name shouldBe "Drakas"
                    team shouldBe "RVSF"
                    flag shouldBe 0
                    score shouldBe 9
                    frag shouldBe 0
                    death shouldBe 0
                    tk shouldBe 0
                    ping shouldBe 12
                    role shouldBe "normal"
                    host shouldBe "127.0.0.1"
                }
              }
          }
      }
    }
  }
}