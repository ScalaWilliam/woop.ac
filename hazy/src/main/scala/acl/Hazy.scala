package acl

import java.util
import acl.EventStore.EventStoreItem
import com.hazelcast.core.MapStore
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.json4s.jackson.Serialization._
import org.json4s.{CustomSerializer, DefaultFormats, Formats}

import scala.beans.BeanProperty
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.xml.PCData

// want to store as an actual object but also as a json object for ease of querying, right? :-)

case class BaseXHCStoreItem(@BeanProperty var eventId: KeyType, @BeanProperty var timestamp: Long,
                            @BeanProperty var kind: String, @BeanProperty var jsonData: String)

object EventStore {

  case class EventStoreItem(@BeanProperty var timestamp: Long, @BeanProperty var jsonData: String)

  def storable(kindName: String) = new Storable[String, EventStoreItem] {
    override def keyAsString(key: String): String = key

    override def kind: String = kindName

    import org.json4s.jackson.Serialization._
    import org.json4s.JsonDSL._
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    implicit def jsonFormats: Formats = DefaultFormats + new CustomSerializer[EventStoreItem]((fmts) => ( {
      case j@JObject(stuff) => EventStoreItem(timestamp = (j \ "timestamp").extract[Long], jsonData = write(j \ "data"))
    }, {
      case EventStoreItem(timestamp, data) => JObject("timestamp" -> JInt(timestamp), "data" -> parse(data))
    }
      ))

    override def toJson(item: EventStoreItem): String = {
      write(item)
    }

    override def fromJson(data: String): EventStoreItem = {
      read[EventStoreItem](data)
    }

    override def stringToKey(string: String): String = string

    override def dbUrl: String = "http://127.0.0.1:8984/rest/purple-haze"
  }
}

class EventStore(eventKind: String) extends BMStore[String, EventStoreItem]()({
  EventStore.storable(eventKind)
}) {

}


trait Storable[K, V] {
  type KeyType = K
  type ValueType = V

  def keyAsString(keyType: K): String

  def stringToKey(string: String): K

  def kind: String

  def toJson(item: V): String

  def fromJson(data: String): V

  def dbUrl: String
}

class BMStore[K, T](implicit val cc: Storable[K, T]) extends MapStore[K, T] {

  import collection.JavaConverters._

  override def deleteAll(keys: util.Collection[K]): Unit = {
    val q = <query xmlns="http://basex.org/rest">
      <text>
        {PCData(
        """declare variable $ids as xs:string external;
          |declare variable $kind as xs:string external;
          |delete node /json[kind = $kind and id = tokenize($ids)]
        """.stripMargin)}
      </text>
      <variable name="ids" value={keys.asScala.map(cc.keyAsString).mkString(" ")}/>
      <variable name="kind" value={cc.kind}/>
    </query>.toString()
    Request.Post(cc.dbUrl)
      .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
      .bodyString(q, ContentType.APPLICATION_XML).execute()
  }

  override def delete(key: K): Unit = {
    Request.Delete(s"${cc.dbUrl}/${cc.kind}/${cc.keyAsString(key)}.json")
      .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=").execute()
  }

  override def store(key: K, value: T): Unit = {
    val bdyString =
      s"""{"id": "${cc.keyAsString(key)}", "kind": "${cc.kind}", "data": ${cc.toJson(value)}}"""
    val r = Request.Put(s"${cc.dbUrl}/${cc.kind}/$key.json")
      .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=").bodyString(bdyString, ContentType.APPLICATION_JSON).execute()

    val response = r.returnResponse()
    if (!(Set(200, 201) contains response.getStatusLine.getStatusCode)) {
      def body = scala.io.Source.fromInputStream(response.getEntity.getContent).mkString
      throw new RuntimeException(s"Failed to store data due to code ${response.getStatusLine.getStatusCode}: $body")
    }
  }

  override def storeAll(map: util.Map[K, T]): Unit = {
    import collection.JavaConverters._
    map.asScala.foreach { case (k, v) => store(k, v) }
  }

  override def loadAll(keys: util.Collection[K]): util.Map[K, T] = {
    val q = <query xmlns="http://basex.org/rest">
      <text>
        {PCData(
        """declare variable $ids as xs:string external;
          |declare variable $kind as xs:string external;
          |<items>{
          |for $json in /json[kind = $kind and id = tokenize($ids)]
          |return <item>
          |{$json/id}
          |{$json/kind}
          |<data>{json:serialize(<json type="object">{$json/data/*}</json>)}</data>
          |</item>
          |}</items>
        """.stripMargin)}
      </text>
      <variable name="ids" value={keys.asScala.map(cc.keyAsString).mkString(" ")}/>
      <variable name="kind" value={cc.kind}/>
    </query>.toString()
    val ex = Request.Post(cc.dbUrl)
      .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=").bodyString(q, ContentType.APPLICATION_XML).execute()
    val xmlDoc = scala.xml.XML.load(ex.returnContent().asStream())
    (xmlDoc \ "item").map { xml =>
      cc.stringToKey(xml \ "id" text) -> {
        cc.fromJson(xml \ "data" text)
      }
    }.toMap.asJava
  }

  override def loadAllKeys(): util.Set[K] = {
    val ks = Request.Post(cc.dbUrl)
      .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=").bodyString(<query xmlns="http://basex.org/rest">
      <text>
        {PCData(
        """declare option output:method 'text';
          |declare variable $kind as xs:string external;
          |string-join(data(/json[kind = $kind]/id), " ")
        """.stripMargin)}
      </text>
      <variable name="kind" value={cc.kind}/>
    </query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString
    ks.split(" ").filter(_.nonEmpty).map(cc.stringToKey).toSet.asJava
  }


  override def load(key: K): T = {
    loadAll(List(key).asJava).asScala.values.headOption match {
      case Some(item) => item
      case None => null.asInstanceOf[T]
    }
  }

}

