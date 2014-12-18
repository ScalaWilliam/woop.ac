declare function local:display-game($game as node(), $has-demo as xs:boolean) as node() {

let $dateTime := adjust-dateTime-to-timezone(xs:dateTime(data($game/@date)), ())
let $date := xs:date($dateTime cast as xs:date)
let $day-ago := adjust-dateTime-to-timezone(current-dateTime() - xs:dayTimeDuration("P1D"), ()) cast as xs:date
let $date-text :=
if ( $date = xs:date(current-date()) ) then (" today")
else if ( $date = $day-ago ) then (" yesterday")
else (" on "|| $date)
let $has-flags := not(empty($game//@flags))

return
<article class="game" style="{"background-image:url('/assets/maps/"||data($game/@map)||".jpg')"}"><div class="w">
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
                                <td class="name">{data($player/@name)}</td>
                            </tr>
                            }
                        </tbody>
                    </table>
                </div>
        }
    </div></div>
</article>
};