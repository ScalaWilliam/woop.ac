package acleague.enrichers

import java.time.ZonedDateTime

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object JodaTimeToZDT {
  def apply(date: DateTime): ZonedDateTime = {
    ZonedDateTime.parse(ISODateTimeFormat.dateTimeNoMillis().print(date))
  }
}
