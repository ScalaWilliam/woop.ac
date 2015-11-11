package acleague.mserver

import java.time.{Instant, ZoneId, ZonedDateTime}

import org.joda.time.DateTimeZone
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatterBuilder, DateTimeFormat}

import scala.util.control.NonFatal

/**
  * Created by William on 11/11/2015.
  */
object ExtractMessage {
  /**
    * @return server identifier & message
    */
  val matcher =
    """Date: (.*), Server: (.*), Payload: (.*)""".r
  val parsers = Array(
    DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss ZZZ yyyy").getParser,
    ISODateTimeFormat.dateTimeNoMillis().getParser,
    ISODateTimeFormat.dateTime().getParser
  )
  val dateFmt = new DateTimeFormatterBuilder().append(null, parsers).toFormatter

  def unapply(line: String): Option[(ZonedDateTime, String, String)] = {
    PartialFunction.condOpt(line) {

      case matcher(date, serverId, message) => try {
        val dat = {
          val jdt = dateFmt.parseDateTime(date).withZone(DateTimeZone.UTC).getMillis
          ZonedDateTime.ofInstant(Instant.ofEpochMilli(jdt), ZoneId.of("UTC"))
        }

        (dat, serverId, message)
      }
      catch {
        case NonFatal(e) =>
          throw new RuntimeException(s"Failed to parse line: $line due to $e", e)
      }
    }
  }
}
