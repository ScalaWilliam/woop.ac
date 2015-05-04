(function () {

    var {PersonProfile, GameCards} = require('./components/ACL.jsx'),
        React = require('react');

    var Tst = React.createClass({
        render() {
            return <div>K</div>;
        }
    });


    global.RenderMe = function(jsonValue) {
        var obj = JSON.parse(jsonValue);
        if ( "achievements" in obj ) {
            return React.renderToString(<PersonProfile profile={obj}/>);
        }
        if ( obj.length && "hasFlags" in obj[0] ) {
            return React.renderToString(<GameCards games={obj}/>);
        }
    };
})();