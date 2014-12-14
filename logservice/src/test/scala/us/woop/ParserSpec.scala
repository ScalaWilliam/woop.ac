import acleague.ingesters._
import org.scalatest.{Matchers, Inspectors, Inside, WordSpec}

class ParserSpec extends WordSpec with Inside with Inspectors with Matchers {
  "Game capture" must {
    "Not fail for an HTF game" in {
      val inputSequence = """
                            |Status at 12-12-2014 20:41:07: 1 remote clients, 0.0 send, 0.3 rec (K/sec)
                            |
                            |Game status: hunt the flag on ac_depot, game finished, open, 2 clients
                            |cn name             team flag score frag death tk ping role    host
                            | 0 Drakas           RVSF    0   9  0     0  0   12 normal  127.0.0.1
                            |   w00p|Sanzo       RVSF    8   33    35  -    - disconnected
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