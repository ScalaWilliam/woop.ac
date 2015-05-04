package acl.hazy

import acl.hazy.HazelcastSource.{EntryListenerId, UpdateEvent}
import akka.actor.{ActorLogging, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import com.hazelcast.core.{EntryEvent, MapEvent, EntryListener, IMap}

import scala.annotation.tailrec

object HazelcastSource {

  case class EntryListenerId(entryListenerId: String)
  
  sealed trait UpdateEvent[K, V]
  case class EntryUpdated[K, V](key: K, value: Option[V], oldValue: Option[V]) extends UpdateEvent[K, V]
  case class EntryEvicted[K, V](key: K, value: Option[V], oldValue: Option[V]) extends UpdateEvent[K, V]
  case class EntryAdded[K, V](key: K, value: Option[V], oldValue: Option[V]) extends UpdateEvent[K, V]
  case class EntryRemoved[K, V](key: K, value: Option[V], oldValue: Option[V]) extends UpdateEvent[K, V]
  case class MapCleared[K, V](event: MapEvent) extends UpdateEvent[K, V]
  case class MapEvicted[K, V](event: MapEvent) extends UpdateEvent[K, V]

  def publisher[K, V](iMap: IMap[K, V], addEntryListener: EntryListener[K, V] => EntryListenerId) = Source.actorPublisher[UpdateEvent[K, V]](Props(new HazelcastSource(iMap, addEntryListener)))

}
import akka.actor.ActorDSL._
class HazelcastSource[K, V](map: IMap[K, V], addEntryListener: EntryListener[K, V] => EntryListenerId) extends ActorPublisher[UpdateEvent[K, V]] with ActorLogging with Act  {

  import akka.stream.actor.ActorPublisherMessage._
  import HazelcastSource._

  case class GotEvent(event: UpdateEvent[K, V])

  lazy val listenerId = addEntryListener(new EntryListener[K, V]() {
    override def entryUpdated(event: EntryEvent[K, V]): Unit =
      self ! GotEvent(EntryUpdated(event.getKey, Option(event.getValue), Option(event.getOldValue)))
    override def entryEvicted(event: EntryEvent[K, V]): Unit =
      self ! GotEvent(EntryEvicted(event.getKey, Option(event.getValue), Option(event.getOldValue)))
    override def mapEvicted(event: MapEvent): Unit =
      self ! GotEvent(MapEvicted(event))
    override def entryAdded(event: EntryEvent[K, V]): Unit =
      self ! GotEvent(EntryAdded(event.getKey, Option(event.getValue), Option(event.getOldValue)))
    override def entryRemoved(event: EntryEvent[K, V]): Unit =
      self ! GotEvent(EntryRemoved(event.getKey, Option(event.getValue), Option(event.getOldValue)))
    override def mapCleared(event: MapEvent): Unit =
      self ! GotEvent(MapCleared(event))
  })


  whenStarting {
    listenerId
  }

  whenStopping {
    map.removeEntryListener(listenerId.entryListenerId)
  }

  val MaxBufferSize = 100
  var buf = Vector.empty[UpdateEvent[K, V]]

  become {
    case GotEvent(e) if buf.size == MaxBufferSize =>
      log.error(s"Buffer overflow, cannot push $e")
    case GotEvent(e) =>
      if (buf.isEmpty && totalDemand > 0)
        onNext(e)
      else {
        buf :+= e
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}
