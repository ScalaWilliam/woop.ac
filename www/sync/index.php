<?php
if ( !isset($_SERVER['SYNC_KEY']) ) die("no SYNC_KEY set");
if ( $_GET['sync-key'] !== $_SERVER['SYNC_KEY'] ) die("provided sync key is invalid");

function bower() {
    echo "Launching bower";
    system("pwd");
    system("bash -c 'pwd && cd .. && pwd && { bower install | xargs echo; }'");
}
if ( $_SERVER['HTTP_HOST'] == "alfa.actionfps.com" ) {
    $post_body = file_get_contents('php://input');
    $json = json_decode($post_body, true);
    if ( strpos($post_body, "www") !== false ) {
        $sha = $json['after'];
        if (ctype_alnum($sha)) {
            system("git fetch");
            system("git checkout $sha");
            bower();
            echo "Completed fetching $sha.\n";
        }
    }
} else {
    system("git checkout master");
    system("git pull");
    bower();
    echo "Complete.\n";
}
