package plugins

import plugins.DataSourcePlugin.UserProfile

import scala.concurrent.Future

trait DataSourcePluginInterface {
  def getEvents: Future[String]
  def getGames: Future[String]
  def getGame(id: String): Future[String]
  def viewUser(userId: String): Future[Option[UserProfile]]
}
