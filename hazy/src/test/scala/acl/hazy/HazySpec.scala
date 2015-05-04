package acl.hazy

import acl.BMStore
import acl.hazy.HazySpec.Tut
import com.hazelcast.core.MapLoader
import org.json4s.{Formats, DefaultFormats}
import org.json4s.jackson.Serialization._
import org.scalatest.{Matchers, WordSpec}

class HazySpec extends WordSpec with Matchers {

  "Hazy" must {
    val store = new BMStore[String, Tut]()(HazySpec.str)
    import collection.JavaConverters._
    "Serialize/deserialize" in {
      val input = Tut(yes = "yes", stuff = Option("K"))
      HazySpec.str.fromJson(HazySpec.str.toJson(input)) shouldBe input
    }
    "Clear all data" in {
      store.deleteAll(store.loadAllKeys())
      store.loadAllKeys().asScala shouldBe empty
    }
    "Add an item" in {
      store.store("test", Tut(yes = "Yes", stuff = Option.empty))
      store.loadAllKeys().asScala should contain only ("test")
    }
    "Delete this item" in {
      store.delete("test")
      store.loadAllKeys().asScala shouldBe empty
    }
    "Add several items" in {
      store.storeAll(
        Map(
          "test3" -> Tut(yes = "Yes3", stuff = Option("whut")),
          "test4" -> Tut(yes = "Yes4", stuff = Option.empty)
        ).asJava
      )
      store.loadAllKeys().asScala should contain only ("test3", "test4")
      store.loadAll(store.loadAllKeys()).asScala should contain only (
        "test3" -> Tut(yes = "Yes3", stuff = Option("whut")),
        "test4" -> Tut(yes = "Yes4", stuff = Option.empty)
      )
      store.load("test3") shouldBe Tut(yes = "Yes3", stuff = Option("whut"))
    }
    "Delete only one of these items" in {
      store.delete("test3")
      store.loadAllKeys().asScala should contain only("test4")
    }
  }
}
object HazySpec {

  case class Tut(yes: String, stuff: Option[String])

  implicit val str = new acl.Storable[String, Tut] {
    override def keyAsString(keyType: String): String = keyType
    override def kind: String = "awut"
    import org.json4s.jackson.Serialization._
    implicit def jsonFormats: Formats = DefaultFormats
    override def toJson(item: Tut): String = {
      write(item)
    }
    override def fromJson(data: String): Tut = {
      read[Tut](data)
    }
    override def stringToKey(string: String): String = string
    override def dbUrl: String = "http://127.0.0.1:8984/rest/purple-haze"
  }

}