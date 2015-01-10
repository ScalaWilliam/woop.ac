package acleague.app
import akka.actor.ActorDSL._
import com.hazelcast.core.HazelcastInstance

object RankingService extends App {
  /**
   * While initialising
   * 1. Fetch user list from BaseX
   * 2. Start indexing everything from scratch
   *
   * While running
   * 1. Notification from Hazelcast about a game
   * 2. Fetch game from BaseX
   * 3. Perform
   *
   * 1. Notification from Hazelcast about a new user
   * 2. Fetch user from BaseX
   * 3. Start indexing everything from scratch
   *
   * Indexing from scratch
   * 1. Fetch fresh list of users (don't need pagination)
   * 2.
   */
}

