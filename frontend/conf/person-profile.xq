declare option output:method 'json';
declare function local:game-header($game as node(), $has-demo as xs:boolean) as item() {

    let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
    let $date := xs:date($dateTime cast as xs:date)
    let $day-ago := adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P1D"), ()) cast as xs:date
    let $ago := xs:dayTimeDuration(current-dateTime() - $dateTime)
    let $date-text :=
        if ( (current-dateTime() - $dateTime) le xs:dayTimeDuration("PT2H") ) then (" just now")
        else if ( $ago le xs:dayTimeDuration("PT12H"))  then (" today")
        else if ( $date = xs:date(current-date()) ) then (" today")
            else if ( $ago le xs:dayTimeDuration("P2D")) then (" yesterday")
                else (" on "|| $date)

    return map {"id": data($game/@id), "mode": data($game/@mode), "map": data($game/@map), "at": $date-text, "has-demo": $has-demo}
};
declare function local:display-game($regs as node()*, $game as node(), $has-demo as xs:boolean) as item() {

    let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
    let $date := xs:date($dateTime cast as xs:date)
    let $day-ago := adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P1D"), ()) cast as xs:date
    let $ago := xs:dayTimeDuration(current-dateTime() - $dateTime)
    let $date-text :=
        if ( (current-dateTime() - $dateTime) le xs:dayTimeDuration("PT2H") ) then (" just now")
        else if ( $ago le xs:dayTimeDuration("PT12H"))  then (" today")
        else if ( $date = xs:date(current-date()) ) then (" today")
            else if ( $ago le xs:dayTimeDuration("P2D")) then (" yesterday")
                else if ( $date = $day-ago ) then (" yesterday")
                    else (" on "|| $date)
    let $has-flags := not(empty($game//@flags))

    return
        map:merge((
            if ( $has-demo ) then (map { "demo": data($game/@id)}) else (),
map {
"id": data($game/@id),
"mode": data($game/@mode),
"map": data($game/@map),
"when": $date-text,
"hasFlags": $has-flags,
"teams": array {
for $team in $game/team[@name = ("RVSF", "CLA")]
let $name := data($team/@name)
let $low-name := lower-case($name)
return
map:merge((
if ( $has-flags ) then (map { "flags": data($team/@flags) }) else (),
map {
"name":  data($team/@name),
"frags": data($team/@frags),
"players":
array {
for $player in $team/player
return map:merge((
map {
"name": data($player/@name),
"frags": data($player/@frags)
},
if ( $has-flags) then (map { "flags": data($player/@flags)}) else (),
let $ru := $regs[@game-nickname = data($player/@name)]
return if ( $ru) then (map{"user":data($ru/@id)}) else ()
))
}
}
))
}
}
))
};

declare variable $user-id as xs:string external;
let $whuts := 
for $u in /registered-user[@id = $user-id]
let $user-record := /user-record[@id= string($u/@id)]
let $record := $user-record
return map {
    "nickname": data($u/@game-nickname),
    "basics": map{
        "time-played": 
            let $time := xs:duration(data($record/counts/@time))
            let $days := days-from-duration($time)
            let $hours := hours-from-duration($time)
            let $vu := 
                if ( $days = 0 and $hours = 0 )
                then ("Not enough")
                else (
                    if ( $days eq 1 ) then ("1 day")
                    else if ( $days gt 1 ) then ($days || " days")
                    else ()," ",
                    if ( $hours eq 1 ) then ("1 hour")
                    else if ( $hours gt 1 ) then ($hours|| " hours")
                    else ()
                )
            return string-join($vu, ' '),
        "flags": data($record/counts/@flags),
        "games-played": data($record/counts/@games),
        "frags": data($record/counts/@frags)
    },
    "achievements":
        let $map-master := $user-record/achievements/capture-master
        let $master-table :=
            array {
                for $completion in $map-master/map-completion
                let $is-completed := $completion/@is-completed = "true"
                return map {
                    "completed": $is-completed,
                    "mode": data($completion/@mode),
                    "map": data($completion/@map),
                    "progress-rvsf": data($completion/@progress-rvsf),
                    "target-rvsf": data($completion/@target-rvsf),
                    "progress-cla": data($completion/@progress-cla),
                    "target-cla": data($completion/@target-cla)
                }
            }
        let $capture-master-achievement :=
            let $achievement := $record/achievements/capture-master
            let $basics := map {
                "achieved": xs:boolean(data($achievement/@achieved)),
                "title": "Capture Master",
                "type": "capture-master",
                "description": "Complete the selected CTF maps, both sides 3 times"
            }
            let $other :=
                if ( $achievement/@achieved = 'true' )
                then (map{ "when":data(/game[@id = data($achievement/@at-game)]/@date) })
                else (map{
                    "total-in-level":data($achievement/@target),
                    "progressInLevel": data($achievement/@progress),
                    "remainingInLevel": data($achievement/@remaining)
                    })
            return map:merge(($basics, $other, map { "table": $master-table }))

        let $progress-achievements := 
            for $achievement in ($record/achievements/flag-master, $record/achievements/frag-master, $record/achievements/cube-addict)
            let $level := data($achievement/@level)
            let $basics := map {
                "achieved": xs:boolean(data($achievement/@achieved)) ,
                "title": 
                    if ( $achievement/self::flag-master ) then ("Flag Master: "||$level)
                    else if ( $achievement/self::frag-master ) then ("Frag Master: "||$level)
                    else if ($achievement/self::cube-addict) then ("Cube Addict: "||$level||"h")
                    else (node-name($achievement)),
                "description":
                    if ($achievement/self::frag-master[@level='500']) then ("Well, that's a start.")
                    else if ($achievement/self::frag-master[@level='1000']) then ("Already lost count.")
                    else if ($achievement/self::frag-master[@level='2000']) then ("I'm quite good at this!")
                    else if ($achievement/self::frag-master[@level='5000']) then ("I've seen blood.")
                    else if ($achievement/self::frag-master[@level='10000']) then ("That Rambo guy got nothin' on me.")
                    else if ($achievement/self::flag-master[@level='50']) then ("What's that blue thing?")
                    else if ($achievement/self::flag-master[@level='100']) then ("I'm supposed to bring this back?")
                    else if ($achievement/self::flag-master[@level='200']) then ("What do you mean it's TDM?")
                    else if ($achievement/self::flag-master[@level='500']) then ("Yeah, I know where it goes.")
                    else if ($achievement/self::flag-master[@level='1000']) then ("Can I keep one at least?")
                    else if ($achievement/self::cube-addict[@level='5']) then ("Hey, this game looks fun.")
                    else if ($achievement/self::cube-addict[@level='10']) then ("I kinda like this game.")
                    else if ($achievement/self::cube-addict[@level='20']) then ("Not stopping now!")
                    else if ($achievement/self::cube-addict[@level='50']) then ("I love this game!")
                    else if ($achievement/self::cube-addict[@level='100']) then ("Just how many hours??")
                    else if ($achievement/self::cube-addict[@level='200']) then ("Wait, when did I start?")
                    else (),
                "type": node-name($achievement),
                "achievement-id": node-name($achievement) ||"-"||data($achievement/@level)
            }
            let $other :=
                if ( $achievement/@achieved = "true" )
                then (map { "when": data(/game[@id = data($achievement/@at-game)]/@date) })
                else (map {
                    "total-in-level": data($achievement/@total-in-level),
                    "progress-in-level": data($achievement/@progress-in-level),
                    "remaining-in-level": data($achievement/@remaining-in-level),
                    "level": data($achievement/@level)
                    })
            return map:merge(($basics, $other))
        let $simple-achievements := (
            let $solo-flagger := $record/achievements/solo-flagger
            return map:merge((map {
            "type": "maverick",
            "title": "Maverick",
            "description":"Achieve all winning team's flags, 5 minimum",
            "achieved": xs:boolean(data($solo-flagger/@achieved))
            }, if ( $solo-flagger/@achieved = "true" ) then (map{"when": data(/game[@id = data($solo-flagger/@at-game)]/@date)}) else (map{})))
            ,
            let $terrible-game := $record/achievements/terrible-game
            return map:merge((map{
            "type":"terrible-game",
            "title":"Terrible Game",
            "description":"Score less than 15 frags.",
            "achieved":xs:boolean(data($terrible-game/@achieved))
            }, if ( $terrible-game/@achieved = "true" ) then (map { "when": data(/game[@id = data($terrible-game/@at-game)]/@date) }) else (map{})))
            ,
            let $slaughterer := $record/achievements/slaughterer
            return map:merge((map{
            "type": "butcher",
            "title": "Butcher",
            "description": "Make over 80 kills in a game.",
            "achieved":xs:boolean(data($slaughterer/@achieved))}, if ( $slaughterer/@achieved = "true" ) then (map { "when": data(/game[@id = data($slaughterer/@at-game)]/@date) }) else ()))
            ,
            let $dday := $record/achievements/dday
            return map:merge((map{"type": "dday", "title": "D-Day", "description": "Play at least 12 games in one day.", "achieved": xs:boolean(data($dday/@achieved))},
            if ( $dday/@achieved = "true" ) then (map{"when": data(/game[@id = data($dday/@at-game)]/@date)}) else (map{})))
            ,
            let $tdm-lover := $record/achievements/tdm-lover
let $basics := map{"type": "tdm-lover", "achieved":xs:boolean(data($tdm-lover/@achieved)), "title": "TDM Lover",
"description":"Play at least " ||data($tdm-lover/@target)||" TDM games."}
let $others := if ( $tdm-lover/@achieved = "true" ) then (map{"when": data(/game[@id = data($tdm-lover/@at-game)]/@date)}) else (
map{"total-in-level":data($tdm-lover/@target), "progress-in-level": data($tdm-lover/@progress), "remaining-in-level": data($tdm-lover/@remain) })
            return map:merge(($basics, $others))
            ,
            let $tosok-lover := $record/achievements/tosok-lover
            let $basics := map{"type": "lucky-luke", "achieved": xs:boolean(data($tosok-lover/@achieved)), "title": "Lucky Luke",
            "description":"Play at least " ||data($tosok-lover/@target)||" TOSOK games."}
            let $others := if ( $tosok-lover/@achieved = "true" ) then (map{"when": data(/game[@id = data($tosok-lover/@at-game)]/@date)}) else (
            map{"total-in-level": data($tosok-lover/@target),
            "progress-in-level": data($tosok-lover/@progress),
            "remaining-in-level": data($tosok-lover/@remain)})
            return map:merge(($basics, $others))
        )
        let $achievements :=
            for $a in ($capture-master-achievement, $simple-achievements, $progress-achievements)
            let $update-when := if ( map:contains($a, 'when' )) then (map{'when': substring(map:get($a, 'when'), 1, 10)}) else (map{})
            let $update-pp :=
                for $total-in-level in map:get($a, 'total-in-level')
                for $progress-in-level in map:get($a, 'progress-in-level')
                let $percentage := xs:int(100 * $progress-in-level div $total-in-level)
                return map { 'progress-percent': $percentage }
            let $nm := map:merge(($a, $update-when, $update-pp))
            order by
                $nm?achieved = true() descending,
                $nm?when descending,
                xs:int((map:get($nm, 'progress-percent'), 0)[1]) descending,
                map:contains($nm, 'progress-in-level') descending
            return $nm
        return array { $achievements } ,
    "recent-games" : array{
        let $all-games := 
            for $pig in $record//played-in-game
            for $game in /game[@id= data($pig/@game-id)]
            order by $game/@date descending
            let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
            return local:game-header($game, $has-demo)
        return $all-games[position() = 1 to 7]
    },
    "youtubes": array {
        for $approved-video in /video-approved
        let $id := data($approved-video/@id)
        for $video in /video
        where $video/@id = $id
        let $games-ids := $video//game
        where ($user-record//played-in-game/@game-id = $games-ids) or ($video//player = data($u/@id))
        order by $video/@published-at descending
        let $games := 
            for $game in subsequence(/game[@id = $games-ids],1,1)
            let $has-demo := exists(/local-demo[@game-id = data($game/@id)])
            return local:game-header($game, $has-demo)
        return map:merge((map{"id": $id}, if ( empty($games)) then (map{}) else (map{"games": $games})))
    }
}
return $whuts
