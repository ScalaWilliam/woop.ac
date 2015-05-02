package plugins

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.Hazelcast
import play.api._
import play.api.inject.ApplicationLifecycle
import javax.inject._
class HazelcastPlugin @Inject()(applicationLifecycle: ApplicationLifecycle) {

  val hazelcast = {
    val instance = Hazelcast.newHazelcastInstance()
    instance
  }

  import concurrent._
  import ExecutionContext.Implicits.global
  applicationLifecycle.addStopHook(() => Future(blocking(hazelcast.shutdown())))

}
