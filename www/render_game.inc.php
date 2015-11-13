<?php
function render_game_team_player($game, $team, $player)
{
    ?>
    <li>
        <?php if ($player['flags']) { ?>
            <span class="score flags"><?php echo $player['flags']; ?></span>
        <?php } ?>
        <span class="subscore frags"><?php echo $player['frags']; ?></span>
        <span class="name"><?php echo htmlspecialchars($player['name']); ?></span>

    </li>
    <?php
}

function render_game_team($game, $team)
{
    ?>
<div class="<?php echo $team['name'] ?> team">
    <div class="team-header">
        <h3><img src="http://woop.ac/assets/<?php echo strtolower($team['name']); ?>.png"/></h3>

        <div class="result">
            <?php if (isset($team['flags'])) { ?>
                <span class="score"><?php echo $team['flags']; ?></span>
            <?php } ?>
            <span class="subscore"><?php echo $team['frags']; ?></span>
        </div>
    </div>
    <div class="players">
        <ol><?php foreach ($team['players'] as $player) {
                render_game_team_player($game, $team, $player);
            } ?>
        </ol>
    </div>
    </div><?php

}

function render_game($game)
{
    ?>
    <article
        class="GameCard game"
        style="background-image: url('http://woop.ac/assets/maps/<?php echo $game['map']; ?>.jpg');">
        <div class="w">
            <header>
                <h2>
                    <a href="/game/?id=<?php echo $game['id']; ?>/">
                        <?php echo $game['mode']; ?>
                        @
                        <?php echo $game['map']; ?></a>

                    <time is="relative-time" datetime="<?php echo $game['gameTime']; ?>">
                        <?php echo $game['gameTime']; ?>
                    </time>
                </h2>
            </header>
            <div class="teams">
                <?php
                foreach ($game['teams'] as $team) {
                    render_game_team($game, $team);
                }
                ?>
            </div>
        </div>

    </article>
<?php } ?>