package acleague.actors

import acleague.Imperative
import acleague.LookupRange.{IpRange, IpRangeOptionalCountry}
import acleague.actors.RankerSecond.{FoundEvent, UpdatedUser}
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.fluent.{Executor, Request}
import org.apache.http.entity.ContentType

import scala.xml.Elem

object RankerActor extends LazyLogging {
  case class NewUser(user: RegisteredUser)
  case class NewGame(gameId: String)
  case class GameEvents(gameId: String, events: Imperative.EmmittedEvents[RegisteredUser])
  val dbName = AppConfig.basexDatabaseName
  lazy val executor = Executor.newInstance()
    .auth("admin", "admin")
  def postDatabaseRequest(input: Elem): String = {
    val inputString = input.toString()
    val startTime = System.currentTimeMillis()
    import concurrent.duration._
    try {
      val output = executor.execute(Request.Post(AppConfig.basexDatabaseUrl)
        .bodyString(inputString, ContentType.APPLICATION_XML)).returnContent().asString()
      val endTime = System.currentTimeMillis()
      val took = (endTime - startTime).millis
      logger.debug(s"Sending query to ${AppConfig.basexDatabaseUrl} took $took: $inputString")
      logger.trace(s"Result was: $output")
      output
    } catch {
      case e: Throwable =>
        val endTime = System.currentTimeMillis()
        val took = (endTime - startTime).millis
        logger.error(s"Sending query to ${AppConfig.basexDatabaseUrl} took $took: $inputString, resulted in Exception $e", e)
        throw e
    }
  }

  def getGames = {
    val xmlData = scala.xml.XML.loadString(postDatabaseRequest(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[
let $all-games :=
  for $game in /game
  let $date := xs:dateTime($game/@date) cast as xs:date
  order by $date ascending
  return $game
let $context-games := subsequence($all-games, 1, 5000)
let $first-game := data(subsequence($context-games, 1, 1)/@id)
let $last-game := data(subsequence($context-games, count($context-games), 1)/@id)
return <games first-game="{$first-game}" last-game="{$last-game}">{$context-games}</games>
]]></rest:text>
    </rest:query>))
    (
      xmlData \@ "first-game",
      xmlData \@ "last-game",
      xmlData.\\("game").toIterator.map(_.asInstanceOf[Elem]).map(Imperative.createGameFromXml)
    )
  }
  def getGame(id:String) = scala.xml.XML.loadString(postDatabaseRequest(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[
    declare variable $id as xs:string external;
    /game[@id=$id]
]]></rest:text>
    <rest:variable name="id" value={id}/>
  </rest:query>)).\\("game").headOption.map(_.asInstanceOf[Elem]).map(Imperative.createGameFromXml)
  case class RegisteredUser(gameName: String, id: String, name: String, countryCode: String)
  def getUsers = for {
    xmlContent <- Option(postDatabaseRequest(<rest:query xmlns:rest="http://basex.org/rest">
    <rest:text><![CDATA[<registered-users>{
    /registered-user}</registered-users>
]]></rest:text>
  </rest:query>)).toIterator
    xml <- Option(scala.xml.XML.loadString(xmlContent)).toIterator
    u <- xml.\\("registered-user").map(_.asInstanceOf[Elem]).toIterator
  } yield RegisteredUser(
      gameName = (u\"@game-nickname").text,
      id = (u\"@id").text,
      name = (u\"@name").text,
      countryCode = (u\"@country-code").text
    )

  def getRanges = for {
    xmlContent <- Option(postDatabaseRequest(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[<ranges>{/range}</ranges> ]]></rest:text>
    </rest:query>)).toIterator
    xml <- Option(scala.xml.XML.loadString(xmlContent)).toIterator
  rangeDef <- xml \\ "range"
  cidr = (rangeDef \ "@cidr").text
  countryCodeO = Option((rangeDef \ "@country-code").text).filter(_.nonEmpty)
  } yield IpRangeOptionalCountry(IpRange(cidr), countryCodeO)

  def saveUserRecord(user: UpdatedUser) = {
    saveUserRecordXml(<user-records>{user.xml}</user-records>)
  }
  def saveUserRecords(user: List[UpdatedUser]) = {
    saveUserRecordXml(<user-records>{user.map(_.xml)}</user-records>)
  }
  def saveUserRecordXml(xmlDoc: scala.xml.Elem) = {
    postDatabaseRequest(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[
      for $user-record in //user-record
      let $id := data($user-record/@id)
      let $existing-record := db:open("]]>{dbName}<![CDATA[")/user-record[@id=$id]
      let $new-user-record :=
        copy $c := $user-record
        modify insert node (attribute {'updated-date'} {current-dateTime()}) into $c
        return $c
      return if ( empty($existing-record) ) then (db:add("]]>{dbName}<![CDATA[", $new-user-record, "manual-user-record"))
      else (replace node $existing-record with $new-user-record)
]]></rest:text>
      <rest:context>{xmlDoc}</rest:context>
    </rest:query>)
  }
  def saveUserEvent(foundEvent: FoundEvent) = {
    saveUserEventXml(foundEvent.toXml)
  }
  def saveUserEvents(foundEvents: List[FoundEvent]) = {
    saveUserEventXml(<events>{foundEvents.map(_.toXml)}</events>)
  }
  def saveUserEventXml(foundEventXml: scala.xml.Elem) = {
    postDatabaseRequest(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text><![CDATA[
      for $new-event in //user-event
      let $game-id := data($new-event/@game-id)
      let $user-id := data($new-event/@user-id)
      let $existing-event :=
        for $event in db:open("]]>{dbName}<![CDATA[")/user-event
        where $event/@game-id = $game-id and $event/@user-id = $user-id
        for $item in $event/*
        for $new-item in $new-event/*
        where deep-equal($item, $new-item)
        return $event
      return if ( not(empty($existing-event)) ) then ()
      else (
        let $new-event-with-tag :=
          copy $c := $new-event
          modify insert node (attribute { 'added-date' } { current-dateTime() }) into $c
          return $c
        return db:add("]]>{dbName}<![CDATA[", $new-event-with-tag, "auto-events")
      )
]]></rest:text>
      <rest:context>{foundEventXml}</rest:context>
    </rest:query>)
  }
  def saveRanges(ranges: Set[IpRangeOptionalCountry]) = {
    postDatabaseRequest(<rest:query xmlns:rest="http://basex.org/rest">
      <rest:text>
        <![CDATA[
    for $range in //range
    let $cidr := $range/@cidr
    let $existing-node := db:open("]]>{dbName}<![CDATA[")/range[@cidr = $range/@cidr]
    return if ( not(exists($existing-node)) ) then (db:add("]]>{dbName}<![CDATA[", $range, "ranges"))
    else (replace node $existing-node with $range)
]]>
      </rest:text>
      <rest:context>
        <ranges>
          {for {IpRangeOptionalCountry(IpRange(cidr), countryCodeO) <- ranges} yield <range cidr={cidr} country-code={countryCodeO.orNull}/>}
        </ranges>
      </rest:context>
    </rest:query>)
  }
}

