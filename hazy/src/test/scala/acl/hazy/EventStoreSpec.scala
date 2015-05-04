package acl.hazy

import java.util.Properties

import acl.{ES2, EventStore}
import acl.EventStore.EventStoreItem
import com.hazelcast.config.{MapStoreConfig, MapConfig, Config}
import com.hazelcast.core.{MapLoader, MapStoreFactory, Hazelcast}
import org.apache.http.client.fluent.Request
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpec}
import collection.JavaConverters._
class EventStoreSpec extends WordSpec with BeforeAndAfterAll with Matchers {

  override protected def beforeAll(): Unit = {
    System.setProperty("hazelcast.wait.seconds.before.join","5")
    super.beforeAll()
  }

  def hazelConfig = {
    val config = new Config()
    config.getNetworkConfig.getJoin.getMulticastConfig.setEnabled(false)
    def tcp = config.getNetworkConfig.getJoin.getTcpIpConfig
    tcp.setEnabled(true)
    tcp.setMembers(List("127.0.0.1").asJava)
    config.getGroupConfig.setName("hazel-testy")
    val mapConfig = new MapConfig("events.*")
    config.addMapConfig(mapConfig)
    val mapStoreConfig = new MapStoreConfig()
    mapConfig.setMapStoreConfig(mapStoreConfig)
    mapStoreConfig.setWriteDelaySeconds(0)
    mapStoreConfig.setFactoryImplementation(new MapStoreFactory[Any, Any]() {
      override def newMapStore(mapName: String, properties: Properties): MapLoader[Any, Any] = {
        val name = mapName.drop("events.".length)
        (new ES2 {
          override def httpUri: String = "http://127.0.0.1:8984/rest/kush"
          override def kind: String = name
          override def auth: (Request) => Request = (_: Request).addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
        }).asInstanceOf[MapLoader[Any, Any]]
      }
    })
    config
  }

  def hazel = Hazelcast.newHazelcastInstance(hazelConfig)

  lazy val hazel1 = hazel

  lazy val hazel2 = hazel

  "Event Store" ignore {

    val es = new ES2 {
      override def httpUri: String = "http://127.0.0.1:8984/rest/kush"
      override def kind: String = "whoa"
      override def auth: (Request) => Request = (_: Request).addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
    }

    import collection.JavaConverters._
    "Clear all items" in {
      es.deleteAll(es.loadAllKeys())
      es.loadAllKeys().asScala shouldBe empty
    }
    "Add three items" in {
      val allItems = ('A' to 'C').map(l => l.toString -> EventStoreItem(timestamp = 1, jsonData = s"""["$l"]""")).toMap
      es.storeAll(allItems.asJava)
      es.loadAll(es.loadAllKeys()).asScala.toMap shouldBe allItems
    }
    "Remove A" in {
      es.delete("A")
      es.loadAll(es.loadAllKeys()) should have size 2
    }
    "Remove B" in {
      es.deleteAll(List("B").asJava)
      es.loadAll(es.loadAllKeys()) should have size 1
    }
  }
  def mapName = "events.kkkk"
  lazy val map1 = hazel1.getMap[String, EventStoreItem](mapName)

  lazy val map2 = hazel2.getMap[String, EventStoreItem](mapName)

  "Hazel bit" must {



    "Ensure both are connected..." in {
      hazel1
      hazel2
      hazel1.getCluster.getMembers.size() shouldBe 2
      hazel2.getCluster.getMembers.size() shouldBe 2
    }

    "Clear all items" in {
      map1.clear()
      map1.size() shouldBe 0
      Thread.sleep(2000)
      map2.size() shouldBe 0
    }
    "Add one item" in {
      val esi = EventStoreItem(timestamp = System.currentTimeMillis(), jsonData = """{"yes": 1}""")
      map1.put("whut", esi)
      Thread.sleep(2000)
      map2.size() shouldBe 1
      map2.get("whut") shouldBe esi
    }
    "Add two items" in {
      val esi = EventStoreItem(timestamp = System.currentTimeMillis(), jsonData = """{"yes": 2}""")
      map1.put("dsa", esi)
      Thread.sleep(2000)
      map2.size() shouldBe 2

      map2.get("dsa") shouldBe esi
    }

  }

  override protected def afterAll(): Unit = {
    Hazelcast.shutdownAll()
    super.afterAll()
  }


}
