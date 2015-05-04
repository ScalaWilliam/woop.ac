var React = require("react");

var MiniGameCard = React.createClass({
    render() {
        var game = this.props.game || this.props;
        var gd;
        if ( game['has-demo']) gd = <a href={`/demos/${game.id}.dmo`} className="demo-link">demo</a>;
        return <li>
            <span>
                <a href={`/game/${game.id}/`}>{game.mode} @ {game.map} {game.at}</a>
                {gd}
            </span>
        </li>;
    }
});
var YouTubeVideoCard = React.createClass({
    render() {
        var video = this.props.video || this.props.video;
        var iframeSrc = `//www.youtube-nocookie.com/embed/${video.id}?rel=0`;
        var game;
        if ( video.game ) game = <p className="game-ln"><MiniGameCard game={video.game}/></p>;
        return <li>
            <iframe allowFullScreen="allowFullScreen" frameBorder="0" src={iframeSrc} height="270" width="480">
            </iframe>
            {game}
        </li>;
    }
});
var PersonProfile = React.createClass({
    render() {
        var person = this.props.profile || this.props.person || this.props;
        var achievements = person.achievements.map((a) => <AchievementCard achievement={a} key={a.title}/>);
        var basics = <table className="basic-counts">
            <tr><th>Time played</th><td>{person.basics['time-played']}</td>
                <th>Flags</th><td>{person.basics.flags}</td></tr>
            <tr><th>Games played</th><td>{person.basics['games-played']}</td>
                <th>Frags</th><td>{person.basics.frags}</td></tr></table>;
        var recentGames = person['recent-games'].map((g) => <MiniGameCard game={g} key={g.game}/>);
        return <div className="profile">
            <h1>{person.nickname}</h1>
            <div className="main-info">
                <div className="basics">{basics}</div>
                <div className="achievements">
                    <h3>Achievements</h3>
                    <div className="achievements">
                        {achievements}
                    </div>
                </div>
            </div>
            <h2>Recent games</h2>
            <ol className="recent-games">
                {recentGames}
            </ol>
            <section id="vids">
                <h2>Mentions on YouTube</h2>
                <p>Upload a video with a description that links to your games or profiles.<br/>
                    <i>Let us know when you upload one so we can approve it.</i>
                </p>
                <ol id="vids">{
                    person.youtubes.map((v) => <YouTubeVideoCard video={v} key={v.id}/>)
                }
                </ol>
            </section>
        </div>

    }
});
var AchievementCard = React.createClass({
    render() {
        var achievement = this.props.achievement || this.props;
        var className = [
            'AchievementCard',
            `achievement-${this.props.achievement.type}`,
            'achievement',
            achievement.achieved ? 'achieved' : 'notAchieved'
        ].join(" ");
        var progStyle = (function(progress) {
            if ( progress <= 50 ) {
                var rightDeg = Math.round(90 + 3.6 * progress);
                return {backgroundImage: "linear-gradient(90deg, #2f3439 50%, rgba(0, 0, 0, 0) 50%, rgba(0, 0, 0, 0)), linear-gradient("+rightDeg+"deg, #ff6347 50%, #2f3439 50%, #2f3439)"};
            } else {
                var leftDeg = Math.round(3.6 * progress - 270);
                return {backgroundImage: "linear-gradient("+leftDeg+"deg, #ff6347 50%, rgba(0, 0, 0, 0) 50%, rgba(0, 0, 0, 0)), linear-gradient(270deg, #ff6347 50%, #2f3439 50%, #2f3439)"};
            }
        })(achievement['progress-percent']);
        var achievementLeft;
        if ( achievement.achieved == true ) {
            achievementLeft = <div className="progress-radial progress-100"><div className="overlay">&#10004;</div></div>
        } else if ( achievement['progress-in-level'] ) {
            achievementLeft = <div className="progress-radial" style={progStyle}><div className="overlay">{achievement['progress-percent']}%</div></div>
        } else {
            achievementLeft =  <div className="progress-radial progress-0"><div className="overlay"></div></div>
        }
        var achievementTable;
        if ( achievement.table ) {
            var maps = achievement.table.map(function (map) {
                return <tr className={map.completed ? 'complete' : 'incomplete'} key={map.map}>
                    <th>{map.mode} @ {map.map}</th>
                    <td className={['cla', map['progress-cla'] == map['target-cla'] ? 'complete' : 'partial'].join(" ")}>{map['progress-cla']}/{map['target-cla']}</td>
                    <td className={['rvsf', map['progress-rvsf'] == map['target-rvsf'] ? 'complete' : 'partial'].join(" ")}>{map['progress-rvsf']}/{map['target-rvsf']}</td>
                </tr>
            });
            achievementTable = <div className="master">
                <table className="map-master">
                    <thead><tr><th>Map</th><th>RVSF</th><th>CLA</th></tr></thead>
                    <tbody>{maps}</tbody></table></div>;
        }
        return <div className={className}>
            <div className="cont">
                <div className="achievement-left">{achievementLeft}</div>
                <div className="achievement-description">
                    <header>
                        <h3>{achievement.title}</h3>
                        <p className="description">{achievement.description}</p>
                    </header>
                    <section className="content">{achievementTable} {this.props.children}</section>
                    <p className="achieved-on">{achievement.when}</p>
                </div>
            </div>
        </div>
    }
});
var LiveEvents = React.createClass({
    render() {
        var theEvents = this.props.events.map(function(event) {
            var userLink = `/player/${event.user}/`;
            var eventId = event.title;
            return <li key={eventId}><a href={userLink}>{event.title}</a> <span className="when">{event.when}</span></li>
        });
        return <ol className="live-events LiveEvents">{theEvents}</ol>;
    }
});
var GameCard = React.createClass({
    render() {
        var game = this.props.game;
        if ( !game && this.props.gameJson ) {
            game = JSON.parse(this.props.gameJson);
        }
        var classes = ['GameCard', 'game', game.isNew ? 'isNew' : '', game.now ? 'isLive' : ''].filter((x) => x.length).join(' ');
        var style = {'backgroundImage': `url('/assets/maps/${ game.map }.jpg')`};
        var gameTops = [];
        if ( game.id ) {
            var gameLink = `/game/${game.id}/`;
            gameTops.push(<a href={gameLink}>{game.mode} @ {game.map} {game.when}</a>);
        } else {
            gameTops.push(<a>{game.mode} @ {game.map} {game.when}</a>);
        }

        if ( game.demo ) {
            gameTops.push(<a className="demo-link" href={`/demos/${game.demo}.dmo`}>demo</a>);
        }
        if ( game.now ) {
            var acLink = `assaultcube://${ game.now.server.canonicalName || game.now.server.server }`;
            gameTops.push(<a className="server-link" href={acLink}>on { game.now.server.shortName }</a>);
            var gameText = `${ game.minRemain } minutes remain`;
            if ( game.minRemain == 1 ) gameText = '1 minute remains';
            if ( game.minRemain == 0 ) gameText = 'game finished';
        }

        var teams = game.teams.filter(function(team) { return team.name == 'CLA' || team.name == 'RVSF'; }).map(function(team) {
            var className = `${ team.name } team`;
            var teamImg = `/assets/${ team.name.toLowerCase() }.png`;
            var teamScore = [];
            if (game.hasFlags) {
                teamScore.push(<span className="score">{team.flags}</span>);
            }
            teamScore.push(<span className="subscore">{team.frags}</span>);
            var players = team.players.map(function(player) {
                var playerScore = [];
                if ( game.hasFlags ) {
                    playerScore.push(<span className="score flags">{ player.flags }</span>);
                }
                playerScore.push(<span className="subscore frags">{ player.frags }</span>);
                var playerBit = <span>{ player.name }</span>;
                if ( player.user ) {
                    var playerLink = `/player/${ player.user }/`;
                    playerBit = <a href={playerLink}>{player.name}</a>;
                }
                var playerId = player.name;
                return <li key={playerId}>
                    {playerScore}
                    <span className="name">{playerBit}</span>
                </li>
            });
            return <div className={className} key={team.name}>
                <div className="team-header">
                    <h3><img src={teamImg}/></h3>
                    <div className="result">{teamScore}</div>
                </div>
                <div className="players">
                    <ol>{players}</ol>
                </div>
            </div>

        });

        var spectatorsCnt = game.teams.filter(team => team.name == 'SPECTATOR').filter(team => team.players.length > 0).map(spectatorsTeam =>
                <div className="spectators">
                    <h4>Spectators:</h4>
                    <ul>{spectatorsTeam.players.map(function(spectator) {
                        var spectatorName = <span>{spectator.name}</span>;
                        if ( spectator.user ) {
                            var userLink = `/player/{ spectator.user }/`;
                            spectatorName = <a href={userLink}>{spectator.name}</a>;
                        }
                        return <li key={spectator.name}>{spectatorName}</li>
                    })}</ul>
                </div>
        ).shift();

        return <article className={classes} style={style}>
            <div className="w">
                <header>
                    <h2>{gameTops}</h2>
                </header>
                <div className="teams">{teams}</div>
                {spectatorsCnt}
            </div>
        </article>;
    }
});

var GameCards = React.createClass({
    render() {
        var stuff = this.props.games.map(function(game) {
            var gameId = game.now ? game.now.server.server : game.id;
            return <GameCard game={game} key={gameId}/>;
        });
        return <div>{stuff}</div>
    }
});

module.exports = {PersonProfile: PersonProfile, GameCards: GameCards};