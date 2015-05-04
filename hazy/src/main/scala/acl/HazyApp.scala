//package acl
//
//import java.util.Properties
//
//import com.hazelcast.config.Config
//import com.hazelcast.core.{MapLoader, MapStoreFactory, Hazelcast}
//
//object HazyApp extends App {
//
//  case class AWut(yes: String, stuff: Option[String])
//
//
  //  val config = new Config()
  //  val mapConfig = config.getMapConfig("*")
  //  val mapStoreConfig = mapConfig.getMapStoreConfig
  //  implicit val str = new acl.Storable[acl.HazyApp.AWut] {
  //    override type KeyType = String
  //    override def keyAsString(keyType: KeyType): String = keyType
  //    override def kind: String = "awut"
  //    import org.json4s.jackson.Serialization._
  //    override def toJson(item: AWut): String = {
  //      write(item)
  //    }
  //    override def fromJson(data: String): AWut = {
  //      read[AWut](data)
  //    }
  //    override def stringToKey(string: String): KeyType = string
  //  }
  //  mapStoreConfig.setFactoryImplementation(new MapStoreFactory[Any, Any]() {
  //    override def newMapStore(mapName: String, properties: Properties): MapLoader[Any, Any] = {
  //      if ( mapName == "wuts") {
  //        (new BMStore[AWut]()).asInstanceOf[MapLoader[Any, Any]]
  //      } else null
  //    }
  //  })
//
//  val hazelcastInstance = Hazelcast.newHazelcastInstance()
//
//  val theMap = hazelcastInstance.getMap[Long, AWut]("wuts")
//
//  theMap.put(123L, AWut("Ok", stuff = Option("adhs")))
//
//  hazelcastInstance.shutdown()
//
//}
