package acleague.actors

import java.io.File

import acleague.Imperative.{Game, User, UserEvent}
import acleague.{Imperative, LookupRange}
import acleague.LookupRange.IpRangeOptionalCountry
import acleague.actors.RankerActor.RegisteredUser
import acleague.actors.RankerSecond._
import akka.actor.ActorDSL._
import akka.actor._
import com.hazelcast.core.{Message, MessageListener, Hazelcast}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Future
import scala.xml.Elem
object AppConfig {
  val conf = ConfigFactory.load()
  val basexDatabaseUrl = conf.getString("acleague.basex.database.url")
  val basexDatabaseName = conf.getString("acleague.basex.database.name")
  val hazelcastGameTopicName = conf.getString("acleague.hazelcast.game.topic")
  val hazelcastUserTopicName = conf.getString("acleague.hazelcast.user.registrations.topic")
  val hazelcastUserUpdateTopicName = conf.getString("acleague.hazelcast.user.update.topic")
  val hazelcastUserEventTopicName = conf.getString("acleague.hazelcast.user.event.topic")
}
object MasterRankerApp extends App with LazyLogging {
  System.setProperty("hazelcast.logging.type", System.getProperty("hazelcast.logging.type", "slf4j"))
  logger.info(s"Application configuration: ${AppConfig.conf}")
  implicit val as = ActorSystem("MR")
  val woot = actor(new Act with ActWithStash {
    import scala.concurrent.duration._

    var masterRanker = context.actorOf(MasterRanker.props, name="master-ranker")
    case object ReloadMasterRanker

    val hazelcast = Hazelcast.newHazelcastInstance()
    case class NewGameIdReceived(gameId: String)
    case class NewUserRegistered(userId: String)

    hazelcast.getTopic[String](AppConfig.hazelcastGameTopicName).addMessageListener(new MessageListener[String] {
      override def onMessage(message: Message[String]): Unit = {
        self ! NewGameIdReceived(message.getMessageObject)
      }
    })

    hazelcast.getTopic[String](AppConfig.hazelcastUserTopicName).addMessageListener(new MessageListener[String] {
      override def onMessage(message: Message[String]): Unit = {
        self ! NewUserRegistered(message.getMessageObject)
      }
    })

    become {
      case ReloadMasterRanker =>
        become {
          case Terminated(subj) if masterRanker == subj =>
            masterRanker = context.actorOf(MasterRanker.props, name="master-ranker")
            unbecome()
            unstashAll()
          case other => stash()
        }
        masterRanker ! Kill
      case NewUserRegistered(userId) =>
        self ! ReloadMasterRanker
      case NewGameIdReceived(gameId) =>
        import context.dispatcher
        import akka.pattern.pipe
        Future {
          for {
            newGame <- RankerActor.getGame(gameId)
          } {
            masterRanker ! NewGameFound(newGame)
          }
        } pipeTo self
      case uu: UpdatedUser =>
        hazelcast.getTopic[String](AppConfig.hazelcastUserUpdateTopicName).publish(uu.userId)
      case fe: FoundEvent =>
        hazelcast.getTopic[String](AppConfig.hazelcastUserEventTopicName).publish(fe.toXml.toString)
    }

    whenStarting {
      context.watch(masterRanker)
      import context.dispatcher
      for {
        g <- RankerActor.getGame("672575197")
      } {
        context.system.scheduler.scheduleOnce(6.seconds, masterRanker, NewGameFound(g))
      }
//      context.system.scheduler.scheduleOnce(10.seconds, self, ReloadMasterRanker)
    }
  })
  as.awaitTermination()
  as.shutdown()
}
object MasterRanker {
  def props = Props(new MasterRanker)
}
class MasterRanker extends Act with ActWithStash with ActorLogging {

  var usersSet: Set[RegisteredUser] = _
  var ipDatabase: Set[IpRangeOptionalCountry] = _
  var ranker: ActorRef = _
  val updatedUsers = collection.mutable.Map.empty[String, scala.xml.Elem]
  val events = collection.mutable.Buffer.empty[FoundEvent]

  /**
   * Start off re-indexing the data that we have already.
   * Then we wait for the games to be re-indexed.
   */
  whenStarting {
    log.info("Starting the master ranker...")
    log.debug("Retrieving users...")
    usersSet = RankerActor.getUsers.toSet
    log.info("Found {} users", usersSet.size)
    log.debug("Retrieving ranges...")
    ipDatabase = RankerActor.getRanges.toSet
    log.info("Found {} ranges", ipDatabase.size)
    ranker = context.actorOf(RankerSecond.props(ipDatabase, usersSet))
    val (firstGameId, lastGameId, games) = RankerActor.getGames
    log.info("Starting to re-index from game ID {} until game ID {}", firstGameId, lastGameId)
    become(reIndexing(lastGameId, Map.empty, List.empty))
    for { game <- games } {
      // in case there are new IPs out there now
      updateIps(game)
      log.debug("Re-indexing game ID {}", game.id)
      ranker ! NewGameFound(game)
    }
  }

  case object RangesCleared
  /**
   * Could fail here.
   */
  def updateIps(newGame: Game): Unit = {
    val unknownIps = newGame.teams.flatMap(_.players).map(_.host).filter(_.nonEmpty).filterNot(host => ipDatabase.exists(_.ipIsInRange(host)))
    val foundRanges = unknownIps.map(unknownIp => LookupRange(unknownIp)).flatten
    ipDatabase = ipDatabase ++ foundRanges
    if ( foundRanges.nonEmpty ) {
      log.debug("Adding new ranges: {}", foundRanges)
      RankerActor.saveRanges(foundRanges)
    }
  }

  def processing: Receive = {
    case newGameFound @ NewGameFound(newGame) =>
      // Ensure that all ranges are clear for our purposes
      log.debug("Found new game ID {}", newGame.id)
      become {
        case RangesCleared =>
          log.debug("Pushing new game ID {}", newGame.id)
          ranker ! newGameFound
          unbecome()
          unstashAll()
        case Status.Failure(reason) =>
          log.error(reason, "Failed to push game ID {} due to {}. We will ignore it.", newGame.id, reason)
          unbecome()
          unstashAll()
        case other => stash()
      }
      import akka.pattern.pipe
      import context.dispatcher
      Future {
        updateIps(newGame)
        RangesCleared
      } pipeTo self
    // and then forward back the processing info
    case freshProcessed: ProcessedGame =>
      log.info("Game pushed: {}", freshProcessed.gameId)
      context.parent ! freshProcessed
    // todo add some nicer failover here/async
    case u: UpdatedUser =>
      log.debug("Updating user record for user ID {}", u.userId)
      RankerActor.saveUserRecord(u)
      context.parent ! u
    case e: FoundEvent =>
      log.debug("Pushing event for game ID {}, user ID {}", e.gameId, e.userId)
      RankerActor.saveUserEvent(e)
      context.parent ! e
  }
  case object ReIndexingCompleted
  def reIndexing(lastGameId: String, pendingUsers: Map[String, UpdatedUser], pendingEvents: List[FoundEvent]): Receive = {
    // stash new games for when it's re-indexed
    case NewGameFound(newGame) =>
      log.debug("New game found {}, stashing", newGame.id)
      stash()
    case processed @ ProcessedGame(`lastGameId`) =>
      // wait for last game to have been processed
      unstashAll()
      context.parent ! processed
      log.info("Re-indexing completed with game ID {}.", lastGameId)
      // continue processing again
      become {
        case ReIndexingCompleted =>
          log.info("Pushing data back has completed fine.")
          log.info("Resuming standard processing after completing game ID {}.", lastGameId)
          become(processing)
          unstashAll()
        case Status.Failure(reason) =>
          log.error(reason, "Failed to save re-indexed data due to {}", reason.getMessage)
          throw reason
        case any => stash()
      }
      // push all events and stuff back up first
      import akka.pattern.pipe
      import context.dispatcher
      log.info("There are {} pending user updates after the re-indexing.", pendingUsers.size)
      log.info("There are {} pending events after the re-indexing.", pendingEvents.size)

      Future {
        RankerActor.saveUserRecords(pendingUsers.values.toList)
        log.info("Saved {} pending user updates.", pendingUsers.size)
        for { r <- pendingUsers.values } context.parent ! r
        RankerActor.saveUserEvents(pendingEvents)
        for { e <- pendingEvents } context.parent ! e
        log.info("Saved {} pending user events.", pendingEvents.size)
        ReIndexingCompleted
      } pipeTo self
    case updatedUser@UpdatedUser(userId, userXml) =>
      become(reIndexing(lastGameId, pendingUsers + (userId -> updatedUser), pendingEvents))
    case event: FoundEvent =>
      become(reIndexing(lastGameId, pendingUsers, pendingEvents :+ event))
    case processed @ ProcessedGame(gameId) =>
      log.debug("Re-indexed game ID {}", gameId)
      context.parent ! processed
  }
}

/** Actor not completely necessary, but we might want to have access to this data nonetheless **/
class RankerSecond(ipDatabase: Set[IpRangeOptionalCountry], registeredUsers: Set[RegisteredUser]) extends Act with ActorLogging {

  val nicknameToCountryUser = {
    for {RegisteredUser(nickname, id, name, countryCode) <- registeredUsers}
    yield nickname -> (countryCode, new User[String](id))
  }.toMap

  val users = {
    for { (nickname, (countryCode, user)) <- nicknameToCountryUser }
    yield user.id -> user
  }

  def ipToCountryCode(ip: String): Option[String] = {
    ipDatabase.find(_.ipIsInRange(ip)).flatMap(_.optionalCountryCode)
  }

  def userLookup(player: acleague.Imperative.Player): Option[acleague.Imperative.User[String]] = {
    for {
      (countryCode, user) <- nicknameToCountryUser.get(player.name)
      if player.host.nonEmpty
      playerCountryCode <- ipToCountryCode(player.host)
      if playerCountryCode == countryCode
    } yield user
  }

  become {
    case NewGameFound(game) =>
      log.debug("Received new game, game ID {}", game.id)
      val acceptanceResult = acleague.Imperative.acceptGame(userLookup)(game)
      log.debug("Game ID {} produced {} user changes and {} events", game.id, acceptanceResult.affectedUsers.size, acceptanceResult.emmittedEvents.size)
      for {
        userId <- acceptanceResult.affectedUsers
        user <- users.get(userId)
        updatedUser = UpdatedUser(userId = userId, xml = user.toXml)
      } { context.parent ! updatedUser }

      for {
        (userId, userEvent) <- acceptanceResult.emmittedEvents
        event = FoundEvent(gameId = game.id, userId = userId, event = userEvent)
      } { context.parent ! event }

      context.parent ! ProcessedGame(game.id)
  }

}

object RankerSecond {
  def props(ipDatabase: Set[IpRangeOptionalCountry], registeredUsers: Set[RegisteredUser]) =
    Props(new RankerSecond(ipDatabase, registeredUsers))
  case class ProcessedGame(gameId: String)
  case class FoundEvent(gameId: String, userId: String, event: UserEvent) {
    def toXml = <user-event game-id={gameId} user-id={userId}>{event.asXml}</user-event>
  }
  case class UpdatedUser(userId: String, xml: Elem)
  case class NewGameFound(game: Game)
  case class Nickname(nickname: String)
  case class IP(ip: String)
  case class CountryCode(countryCode: String)
  case class Options(getUser: Nickname => IP => Option[Username])
  case class Username(username: String)
}