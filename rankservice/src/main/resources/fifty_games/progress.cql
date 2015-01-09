MATCH (u: user)
MERGE (u)-[:ongoing]->(achievement:fifty_games)
SET achievement.target = 50
SET achievement.games = 0
SET achievement.remain = achievement.target - achievement.games
WITH u, achievement
MATCH (u) <- [:is_user]-(p:player), (p)<-[:has_player]-(g:game)
WHERE NOT (()-[:completed]->(:fifty_games)-[:includes]->(p))
MERGE (achievement)-[:includes]->(p)
WITH u, achievement, length(collect(g)) AS number_of_games
SET achievement.games = number_of_games
SET achievement.remain = achievement.target - achievement.games;