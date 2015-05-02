package plugins

import akka.actor.Kill
import org.apache.commons.net.util.SubnetUtils
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Akka
import play.api.libs.ws.WS
import play.api.{Play, Plugin, Application}
import akka.actor.ActorDSL._
import plugins.RangerPlugin.IpRange
import scala.concurrent._
import concurrent.duration._
import javax.inject._
@Singleton
class RangerPlugin @Inject()(applicationLifecycle: ApplicationLifecycle, basexProviderPlugin: BasexProviderPlugin) {

  import ExecutionContext.Implicits.global
  implicit lazy val system = Akka.system(Play.current)

  case class RangeExists(ip: String)

  def rangeExists(ip: String): Future[Boolean] = {
    import akka.pattern.ask
    akky.ask(RangeExists(ip))(5.seconds).mapTo[Boolean]
  }

  case object UpdateRanges
  lazy val akky = actor(new Act {
    var ranges: Set[IpRange] = Set.empty
    whenStarting {
      context.system.scheduler.schedule(0.seconds, 1.minute, self, UpdateRanges)
    }
    case class RangesFound(ranges: Set[IpRange])
    become {
      case RangesFound(r) =>
        ranges = r
      case RangeExists(ip) =>
        sender() ! ranges.exists(_.ipIsInRange(ip))
      case UpdateRanges =>
        import akka.pattern.pipe
        getRanges.map(RangesFound.apply) pipeTo self
    }
  })

  def getRanges = for {
    xmlContent <- basexProviderPlugin.query(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[<ranges>{/range}</ranges> ]]></rest:text>
    </rest:query>)
  } yield {
    val rangesSeq = xmlContent.xml \\ "@cidr" map (cidr => IpRange(cidr.text))
    rangesSeq.toSet
  }
    akky ! UpdateRanges

  applicationLifecycle.addStopHook(() => {
  import concurrent._
  import ExecutionContext.Implicits.global
  Future(blocking(akky ! Kill))})
}

object RangerPlugin {

  case class IpRange(cidr: String) {
    if ( cidr.isEmpty ) {
      throw new IllegalArgumentException("Input was empty. Illegal.")
    }
    lazy val inf = new SubnetUtils(cidr).getInfo
    def ipIsInRange(ip: String) = {
      inf.isInRange(ip) || inf.getAddress == ip
    }
  }

}