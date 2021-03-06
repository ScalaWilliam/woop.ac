<?php
require_once("../render.inc.php");
require("../render_game.inc.php");
?>
<article id="questions">
    <h2>What is this?</h2>
    <p>This is a clan match recording system for <a href="http://woop.us/" target="_blank">Woop Clan's</a> <a href="http://assault.cubers.net/" target="_blank">AssaultCube</a> servers.</p>
    <h2>Is this project open source?</h2>
    <p>Yes, see GitHub: <a href="http://github.com/ScalaWilliam/woop.ac" target="_blank">ScalaWilliam/woop.ac</a>. Contributions, issues, pull requests are welcome.</p>
    <h2>What's the magic behind woop.ac?</h2>
    <p>The genius of <a target="_blank" href="https://www.scalawilliam.com/">William Narmontas, professional software engineer</a>.<br/>
        Technology-wise, it's Play, Scala, Akka and PHP.</p>
    <h2>AssaultCube</h2>
    <iframe width="560" height="315" src="//www.youtube-nocookie.com/embed/1k5sI1Rz558?rel=0" frameborder="0" allowfullscreen></iframe>
    <h2>I'd like to talk to you</h2>
    <p>Find us on <a href="http://www.teamspeak.com/?page=downloads">TeamSpeak</a> at our server <a href="ts3server://aura.woop.ac" target="_blank">&quot;aura.woop.ac&quot;</a>.</p>
    <p>Also join us on <a href="https://webchat.gamesurge.net/?channels=woop-clan">#woop-clan @ GameSurge</a> IRC channel.</p>
    <h2>Which servers do you record from?</h2>
    <p>See the <a href="servers">servers</a> page.</p>
    <h2>Any rules for play?</h2>
    <p>Be fair.</p>
    <h2>What are your plans?</h2>
    <p>Expand. We want your participation.</p>
    <h2>Why Google sign in?</h2>
    <p>Saves us time in development.</p>
    <h2>Why is the homepage slow?</h2>
    <p>It's fast on Google Chrome. We are using Polymer/Web Components which save us time.</p>
</article>

<?php echo $foot; ?>