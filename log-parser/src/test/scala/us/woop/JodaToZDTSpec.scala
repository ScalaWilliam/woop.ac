package us.woop

import java.time.{ZoneOffset, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import acleague.enrichers.JodaTimeToZDT
import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by William on 11/11/2015.
  */
class JodaToZDTSpec extends WordSpec with Matchers {
  "it" must {
    "work" in {
      val A = DateTime.now()
      val B = ZonedDateTime.now(ZoneOffset.UTC).withNano(0)
      JodaTimeToZDT.apply(A) shouldBe B
    }
  }
}
