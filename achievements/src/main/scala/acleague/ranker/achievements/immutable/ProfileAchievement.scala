package acleague.ranker.achievements.immutable
import play.api.libs.json._

trait EventAchieved {
  def eventTitle(name: String): String
}

trait ProfileAchievement {
  def title: String
  def description: String
  def genericInfo: JsObject = JsObject(Map("title" -> JsString(title), "description" -> JsString(description)))
  def info: JsObject
  def asJson: JsObject = genericInfo ++ info ++ {
    this match {
      case _: AchievedProfileAchievement =>
    }
  }
}
trait AchievedProfileAchievement extends ProfileAchievement
trait ProgressingProfileAchievement extends ProfileAchievement
