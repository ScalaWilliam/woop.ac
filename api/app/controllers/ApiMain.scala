package controllers

import java.io.File
import javax.inject._

import play.api.Configuration
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, Controller}


@Singleton
class ApiMain @Inject()(configuration: Configuration) extends Controller {
  val file = new File(configuration.underlying.getString("af.games.path"))
  val games = scala.io.Source.fromFile(file).getLines.map(_.split("\t").toList).collect {
    case List(id, "GOOD", "", json) => id -> Json.parse(json)
  }.take(10).toList

  def recent = Action {
    Ok(JsArray(games.map { case (_, json) => json }))
  }

}