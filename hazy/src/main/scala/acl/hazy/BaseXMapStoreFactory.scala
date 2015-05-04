package acl.hazy

import java.util.Properties

import com.hazelcast.core.{MapLoader, MapStoreFactory}
import org.apache.http.client.fluent.Request

trait BaseXMapStoreFactory extends MapStoreFactory[Any, Any] {
  bx =>
  def databaseUri: String
  def namePrefix(mapName: String): String = mapName.drop("events.".length)
  def auth(request: Request) = request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
  lazy val basexConnector = new BaseXConnector[Request] {
    override def httpUri: String = databaseUri
    override def auth(request: Request): Request = bx.auth(request)
  }
  override def newMapStore(mapName: String, properties: Properties): MapLoader[Any, Any] = {
    (new ES2(kind = namePrefix(mapName), baseXConnector = basexConnector)).asInstanceOf[MapLoader[Any, Any]]
  }
}
