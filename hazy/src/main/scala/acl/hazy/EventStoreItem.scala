package acl.hazy

import scala.beans.BeanProperty

case class EventStoreItem(@BeanProperty var timestamp: Long, @BeanProperty var jsonData: String)
