package ac.woop.client

import ac.woop.PersistentRepository
import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import org.h2.mvstore.MVStore

object SimpleTesterApp extends App {

  val database = new PersistentRepository(new MVStore.Builder().readOnly().fileName("main.db").open())
  implicit val as = ActorSystem("Leet")
  val repository = database.loadRepository
  val firstServerId = repository.servers.keySet.head
  val cl = actor(new MasterCClient(firstServerId, repository))
}