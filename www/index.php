<?php
require_once("render.inc.php");
require("render_game.inc.php");

$games = json_decode(file_get_contents("http://alfa.actionfps.com/recent/"), true);

?><div id="live-events">Events for later...!</div><div id="games"><div id="existing-games"><?php
foreach($games as $game) {
    render_game($game);
}

?></div></div><?php

echo $foot;