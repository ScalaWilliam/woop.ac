declare function local:game-header($game as node(), $has-demo as xs:boolean) as node() {

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

    return
            <span><a href="{"/game/"||data($game/@id)}">{data($game/@mode)} @ {data($game/@map)} {$date-text}</a>
                {
                    if ( $has-demo ) then (<a class="demo-link" href="{"/demos/"||data($game/@id)||".dmo"}">demo</a>) else ()
                }</span>
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