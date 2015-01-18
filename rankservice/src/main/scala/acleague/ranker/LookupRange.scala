package acleague.ranker

import java.io.File
import java.net.InetAddress

import com.maxmind.geoip2.DatabaseReader
import org.apache.commons.net.util.SubnetUtils
import org.apache.http.client.fluent.Request
import org.apache.http.client.utils.URIBuilder

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
