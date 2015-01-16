package acleague

import java.io.File
import java.net.InetAddress

import acleague.LookupRange.IpRangeCountryCode
import com.maxmind.geoip2.DatabaseReader
import org.apache.commons.net.util.SubnetUtils
import org.apache.http.client.fluent.Request
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType

import scala.xml.Elem

/**
 * Created by William on 10/01/2015.
 */
object LookupRange {
  case class IpRange(cidr: String) {
    if ( cidr.isEmpty ) {
      throw new IllegalArgumentException("Input was empty. Illegal.")
    }
    lazy val inf = new SubnetUtils(cidr).getInfo
    def ipIsInRange(ip: String) = {
      inf.isInRange(ip) || inf.getAddress == ip
    }
  }
  case class IpRangeOptionalCountry(range: IpRange, optionalCountryCode: Option[String]) {
    def ipIsInRange(ip: String) =
      range.ipIsInRange(ip)
  }
  case class IpRangeCountryCode(range: String, countryCode: String) {
    lazy val inf = new SubnetUtils(range).getInfo
    def ipIsInRange(ip: String) = {
      inf.isInRange(ip) || inf.getAddress == ip
    }
  }
  /** Find out the country and the IP range of an IP **/
  def ipToLong(ip: InetAddress) =  {
    var result = 0L
    for { octet <- ip.getAddress } {
      result = result << 8
      result = result | (octet & 0xff)
    }
    result
  }
  def apply(ip: String): Option[IpRangeOptionalCountry] = {
    val (rangeO, countryCodeO) = parseXmlToRange(queryXmlForIp(ip))
    for {
      range <- rangeO
    } yield IpRangeOptionalCountry(IpRange(range), lookupCountryByIp(ip) orElse countryCodeO)
  }
//  def apply(ip: String): Option[IpRangeCountryCode] = {
//    val (rangeO, countryCodeO) = parseXmlToRange(queryXmlForIp(ip))
//    for { range <- rangeO; countryCode <- countryCodeO orElse lookupCountryByIp(ip) } yield IpRangeCountryCode(range, countryCode)
//  }
  private def queryXmlForIp(theIp: String) = {
    val builder = new URIBuilder( """https://rest.db.ripe.net/search.xml?source=ripe-grs&source=afrinic-grs&source=apnic-grs&source=arin-grs&source=lacnic-grs&source=jpirr-grs&source=radb-grs""")
    builder.addParameter("query-string", theIp)
    val theUri = builder.build()
    scala.xml.XML.load(Request.Get(theUri).execute().returnContent().asStream())
  }
  lazy val reader = {
    val database = new File(scala.util.Properties.userHome, "GeoLite2-Country.mmdb")
    new DatabaseReader.Builder(database).build()
  }
  private def lookupCountryByIp(ip: String): Option[String] = {
    for {
      country <- Option(reader.country(InetAddress.getByName(ip)))
      code <- Option(country.getCountry.getIsoCode)
    } yield code
  }
  private def parseXmlToRange(result: Elem) = {
    val countryCode = (result \\ "attribute").filter(a => (a \ "@name").text == "country").map(a => (a \ "@value").text).toSet
    val route = (result \\ "attribute").filter(a => (a \ "@name").text == "route").map(a => (a \ "@value").text).toSet
    val inetNum = (result \\ "attribute").filter(a => (a \ "@name").text == "inetnum").map(a => (a \ "@value").text).toSet
    val thingy = """(.*) - (.*)""".r
    val ourRange = inetNum.collect {
      case thingy(from, to) =>
        val fromIp = ipToLong(InetAddress.getByName(from))
        val toIp = ipToLong(InetAddress.getByName(to))
        val nam = Math.ceil(32 - (Math.log(toIp - fromIp + 1) / Math.log(2))).toInt
        s"$from/$nam"
    }.toSet
    val calculatedRange = route.headOption orElse ourRange.headOption
    val calculatedCountryCode = countryCode.headOption
    (calculatedRange, calculatedCountryCode)
  }
}
//object RangeLocator extends App {
////  println(LookupRange("216.195.187.92"))
//  println(new SubnetUtils("83.220.238.0/23").getInfo.getAddress == "83.220.238.0")
//}
//object RangeLocators extends App {
//  val theIp = """213.186.33.6"""
//
//  val inputQuery = <rest:query xmlns:rest="http://basex.org/rest">
//    <rest:text><![CDATA[
//distinct-values(//player[@host != '']/@host)
//]]></rest:text>
//  </rest:query>
//  val result = Request.Post("http://odin.duel.gg:1238/rest/acleague")
//    .bodyString(s"$inputQuery", ContentType.APPLICATION_XML)
//    .execute().returnContent().asString()
//  val ranges = {
//    val ranges = collection.mutable.Set.empty[IpRangeCountryCode]
//    val fetchRangesQuery = <rest:query xmlns:rest="http://basex.org/rest">
//      <rest:text><![CDATA[<ranges>{/range}</ranges> ]]></rest:text>
//    </rest:query>
//    val getResult = Request.Post("http://odin.duel.gg:1238/rest/acleague")
//      .bodyString(s"$fetchRangesQuery", ContentType.APPLICATION_XML)
//      .execute().returnContent().asString()
//    val xmlData = scala.xml.XML.loadString(getResult)
//    val stuffs = for {
//      rangeDef <- xmlData \\ "range"
//      cidr = (rangeDef \ "@cidr").text
//      countryCode = (rangeDef \ "@country-code").text
//    } yield IpRangeCountryCode(cidr, countryCode)
//    ranges ++= stuffs
//    ranges
//  }
//  println(ranges)
////  val ranges = scala.collection.mutable.Set.empty[IpRangeCountryCode]
////  val ips = result.split(" ").toSet.take(3)
//  val ips = result.split(" ").toSet
////  val ips = Set("88.130.0.1", "88.130.0.2", "88.130.0.55")
//  for {
//    ip <- ips
//    if !ranges.exists(_.ipIsInRange(ip))
//    rng <- LookupRange(ip)
//  } { ranges += rng }
//
//  val pushBackQuery = <rest:query xmlns:rest="http://basex.org/rest">
//    <rest:text><![CDATA[
//    for $range in //range
//    let $cidr := $range/@cidr
//    let $existing-node := db:open("acleague")/range[@cidr = $range/@cidr]
//    return if ( not(exists($existing-node)) ) then (db:add("acleague", $range, "ranges"))
//    else (replace node $existing-node with $range)
//]]></rest:text>
//    <rest:context>
//      <ranges>{for {IpRangeCountryCode(cidr, countryCode) <- ranges} yield <range cidr={cidr} country-code={countryCode}/>}</ranges>
//    </rest:context>
//  </rest:query>
//
//  val pushResult = Request.Post("http://odin.duel.gg:1238/rest/acleague")
//    .bodyString(s"$pushBackQuery", ContentType.APPLICATION_XML)
//    .execute().returnContent().asString()
//
//  println(pushResult)
//
//  /*
//  <attribute value="GB" name="country"/>
//  <attribute value="77.44.45.0/24" name="route"/>
//  */
//
//}
