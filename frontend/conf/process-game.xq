declare function local:game-header($game as node(), $has-demo as xs:boolean) as node() {

    let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
    let $date := xs:date($dateTime cast as xs:date)
    let $day-ago := adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P1D"), ()) cast as xs:date
    let $date-text :=
        if ( $date = xs:date(current-date()) ) then (" today")
        else if ( $date = $day-ago ) then (" yesterday")
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
let $date-text :=
if ( $date = xs:date(current-date()) ) then (" today")
else if ( $date = $day-ago ) then (" yesterday")
else (" on "|| $date)
let $has-flags := not(empty($game//@flags))

return
    map:merge((
        if ( $has-demo ) then (map { "demo": data($game/@id)}) else (),
        map {
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
                                        if ( $has-flags) then (map { "flags": data($team/@flags)}) else (),
                                        let $ru := $regs[@game-nickname = data($player/@name)]
                                        return if ( $ru) then (map{"user":data($ru/@id)}) else ()
                                    ))
                                }
                            }
                        ))
            }
        }
))
        (:<article class="game" style="{"background-image:url('/assets/maps/"||data($game/@map)||".jpg')"}"><div class="w">
    <header><h2><a href="{"/game/"||data($game/@id)}">{data($game/@mode)} @ {data($game/@map)} {$date-text}</a>
        {
            if ( $has-demo ) then (<a class="demo-link" href="{"/demos/"||data($game/@id)||".dmo"}">demo</a>) else ()
        }</h2></header>
    <div class="teams">
        {
            for $team in $game/team[@name]
            let $name := data($team/@name)
            let $low-name := lower-case($name)
            return
                <div class="{$low-name || " team"}">
                    <div class="team-header">
                        <h3><img src="{"/assets/"||$low-name||".png"}"/></h3>
                        <div class="result">
                            <span class="score">{if ( $has-flags ) then (data($team/@flags)) else (data($team/@frags))}</span>
                            {if ( $has-flags ) then (<span class="subscore">{data($team/@frags)}</span>) else ()}
                        </div>
                    </div>
                    <table class="players">
                        <tbody>
                            { for $player in $team/player
                            return <tr>
                                <th class="score">{if ( $has-flags ) then (data($player/@flags)) else (data($player/@frags))}</th>
                                {if ( $has-flags ) then (<th class="subscore">{data($player/@frags)}</th>) else ()}
                                <td class="name">{
                                    let $name := data($player/@name)
                                    return subsequence((
                                    for $rp in $regs[@game-nickname = $name]
                                    return <a href="{"/player/"||data($rp/@id)}">{$name}</a>,
                                    <span>{$name}</span>),1,1
                                    )

                                }</td>
                            </tr>
                            }
                        </tbody>
                    </table>
                </div>
        }
    </div></div>
</article>:)
};