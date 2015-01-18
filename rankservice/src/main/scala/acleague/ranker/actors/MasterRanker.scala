package acleague.ranker.actors

import acleague.ranker.LookupRange
import acleague.ranker.LookupRange.IpRangeOptionalCountry
import acleague.ranker.achievements.Imperative.Game
import acleague.ranker.actors.RankerActor.RegisteredUser
import acleague.ranker.actors.RankerSecond.{UpdatedUser, ProcessedGame, NewGameFound, FoundEvent}
import akka.actor.ActorDSL._
import akka.actor.{Props, Status, ActorRef, ActorLogging}

import scala.concurrent.Future

/**
 * Created by William on 18/01/2015.
 */
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
    val (firstGameId, lastGameId, games) = RankerActor.getGames()
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
      becomeStacked {
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

object MasterRanker {
  def props = Props(new MasterRanker)
}