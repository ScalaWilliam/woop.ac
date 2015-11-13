<?php
require_once("../render.inc.php");
require("../render_game.inc.php");

$games = json_decode(file_get_contents("http://alfa.actionfps.com/recent/"), true);
foreach($games as $game) {
    if ( $game['id'] == $_GET['id'] ) {
        ?>
<div id="game"><?php
        render_game($game); ?></div><?php
    }
}
echo $foot;