package acl.hazy

import com.hazelcast.config.{Config, MapConfig, MapStoreConfig}
import com.hazelcast.core.Hazelcast
import org.apache.http.client.fluent.Request
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.collection.JavaConverters._
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
    mapStoreConfig.setFactoryImplementation(new BaseXMapStoreFactory {
      override def databaseUri: String = "http://127.0.0.1:8984/rest/kush"
    })
    config
  }

  def hazel = Hazelcast.newHazelcastInstance(hazelConfig)

  lazy val hazel1 = hazel

  lazy val hazel2 = hazel

  "Event Store" must {

    val es = new EventMapStore(kind = "whoa", baseXConnector = new BaseXConnector[Request] {
      override def httpUri: String = "http://127.0.0.1:8984/rest/kush"
      override def auth(request: Request): Request = request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
    })

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
      map1
      map2
      hazel1.getCluster.getMembers.size() shouldBe 2
      hazel2.getCluster.getMembers.size() shouldBe 2
    }

    "Clear all items" in {
      // wtf, have to use map1.size() ???
      info(s"Clearing items etc; map1 size is ${map1.size()}")
      map1.clear()
      map1.size() shouldBe 0
      map2.size() shouldBe 0
    }
    "Add one item" in {
      val esi = EventStoreItem(timestamp = System.currentTimeMillis(), jsonData = """{"yes": 1}""")
      map1.put("whut", esi)
      map2.size() shouldBe 1
      map2.get("whut") shouldBe esi
    }
    "Add two items" in {
      val esi = EventStoreItem(timestamp = System.currentTimeMillis(), jsonData = """{"yes": 2}""")
      map1.put("dsa", esi)
      map2.size() shouldBe 2
      map2.get("dsa") shouldBe esi
    }

  }

  override protected def afterAll(): Unit = {
    info("Shutting down now...")
    Hazelcast.shutdownAll()
    super.afterAll()
  }


}
