package acleague.ranker.app

import com.typesafe.config.ConfigFactory

/**
 * Created by William on 18/01/2015.
 */
object AppConfig {
  val conf = ConfigFactory.load()
  val basexDatabaseUrl = conf.getString("acleague.basex.database.url")
  val basexDatabaseName = conf.getString("acleague.basex.database.name")
  val hazelcastGameTopicName = conf.getString("acleague.hazelcast.game.topic")
  val hazelcastUserTopicName = conf.getString("acleague.hazelcast.user.registrations.topic")
  val hazelcastUserUpdateTopicName = conf.getString("acleague.hazelcast.user.update.topic")
  val hazelcastUserEventTopicName = conf.getString("acleague.hazelcast.user.event.topic")
}
