package controllers

import java.io.File
import javax.inject._

import acleague.enrichers.JsonGame
import acleague.ranker.achievements.immutable.{EventAchieved, AchievedProfileAchievement, Combined}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, Controller}

import scala.collection.mutable

@Singleton
class ApiMain @Inject()() extends Controller {
  val file = new File("C:\\Users\\William\\woop.ac\\all-combined.games.sorted.log")
  val games = scala.io.Source.fromFile(file).getLines.map(_.split("\t").toList).collect {
    case List(id, "GOOD", "", json) => id -> Json.parse(json)
  }.take(10).toList

  def recent = Action {
    Ok(JsArray(games.map { case (_, json) => json }))
  }

  val ach = {
    val ha = mutable.Buffer.empty[AchievedProfileAchievement]
    val ba = mutable.Buffer.empty[EventAchieved]
    var comb = Combined.empty
    games.foreach { case (_, jsonObject) =>
      val parsedGame = JsonGame.parse(jsonObject).get
      parsedGame.teams.foreach { team =>
        team.players.foreach { player =>
          comb.include(parsedGame, team, player).foreach {
            case (nComb, newProfileAchievements, newEvents) =>
              comb = nComb
              ha ++= newProfileAchievements
              ba ++= newEvents
          }
        }
      }
    }
    (comb, ha.toList, ba.toList)
  }

  def achievements(id: String) = Action {
    Ok(s"Got it! s$ach")
  }
}