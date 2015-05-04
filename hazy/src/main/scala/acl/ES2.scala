package acl

import java.util

import acl.EventStore.EventStoreItem
import com.hazelcast.core.MapStore
import org.apache.http.client.HttpResponseException
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType

import scala.language.postfixOps
import scala.xml.PCData


import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.jackson.JsonMethods._

trait ES2 extends MapStore[String, EventStoreItem] {

  def httpUri: String
  def auth: Request => Request
  def kind: String

  import collection.JavaConverters._

  override def deleteAll(keys: util.Collection[String]): Unit = {
    val q = <query xmlns="http://basex.org/rest">
      <text>
        {PCData(
        """declare variable $ids as xs:string external;
          |declare variable $kind as xs:string external;
          |delete node /json[kind = $kind and id = tokenize($ids)]
        """.stripMargin)}
      </text>
      <variable name="ids" value={keys.asScala.mkString(" ")}/>
      <variable name="kind" value={kind}/>
    </query>.toString()
    auth(Request.Post(httpUri))
      .bodyString(q, ContentType.APPLICATION_XML).execute()
  }
  override def store(key: String, value: EventStoreItem): Unit = {
    implicit val fmts = DefaultFormats
    val bdyString = write(JObject(
      "id" -> JString(key),
      "kind" ->  JString(kind),
      "timestamp" -> JInt(value.timestamp),
      "created-at" -> JString(java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_INSTANT)),
      "data" -> parse(value.jsonData)
    ))
    val r = auth(Request.Put(s"$httpUri/$kind/$key.json")).bodyString(bdyString, ContentType.APPLICATION_JSON).execute()
    val response = r.returnResponse()
    if (!(Set(200, 201) contains response.getStatusLine.getStatusCode)) {
      def body = scala.io.Source.fromInputStream(response.getEntity.getContent).mkString
      throw new RuntimeException(s"Failed to store data due to code ${response.getStatusLine.getStatusCode}: $body")
    }
  }

  override def delete(key: String): Unit = {
    auth(Request.Delete(s"$httpUri/$kind/$key.json")).execute().returnResponse()
  }

  override def storeAll(map: util.Map[String, EventStoreItem]): Unit = {
    map.asScala.foreach { case (k, v) => store(k, v) }
  }

  override def loadAll(keys: util.Collection[String]): util.Map[String, EventStoreItem] = {
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
          |{$json/timestamp}
          |<data>{
          |let $un :=
          |  copy $dd := $json/data
          |  modify (rename node $dd as 'json')
          |  return $dd
          |return json:serialize($un)}</data>
          |</item>
          |}</items>
        """.stripMargin)}
      </text>
      <variable name="ids" value={keys.asScala.mkString(" ")}/>
      <variable name="kind" value={kind}/>
    </query>.toString()
    val ex = auth(Request.Post(httpUri)).bodyString(q, ContentType.APPLICATION_XML).execute()
    val xmlDoc =  scala.xml.XML.load(ex.returnContent().asStream())
    (xmlDoc \ "item").map { xml =>
      (xml \ "id" text) -> {
        EventStoreItem(
          timestamp = (xml \ "timestamp").text.toLong,
          jsonData = {
            implicit val df = DefaultFormats
            write(parse(xml \ "data" text))
          })
      }
    }.toMap.asJava
  }

  override def loadAllKeys(): util.Set[String] = {
    val ks = auth(Request.Post(httpUri)).bodyString(<query xmlns="http://basex.org/rest">
      <text>
        {PCData(
        """declare option output:method 'text';
          |declare variable $kind as xs:string external;
          |string-join(data(/json[kind = $kind]/id), " ")
        """.stripMargin)}
      </text>
      <variable name="kind" value={kind}/>
    </query>.toString(), ContentType.APPLICATION_XML).execute().returnContent().asString
    ks.split(" ").filter(_.nonEmpty).toSet.asJava
  }

  override def load(key: String): EventStoreItem = {
    loadAll(List(key).asJava).asScala.values.headOption match {
      case Some(item) => item
      case None => null.asInstanceOf[EventStoreItem]
    }
  }
}
