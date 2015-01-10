MATCH (u: user), (m: good_map)
WHERE NOT ((u)-[:completed]->(:map_completed_achievement)-[:of_map]->(m))
MERGE (u)-[:ongoing]->(achievement: map_completed_achievement)-[:of_map]->(m)
SET achievement.rvsfTarget = 2
SET achievement.claTarget = 2
WITH u, m, achievement
OPTIONAL MATCH (u)<-[:is_user]-(p:player)<-[:has_player]-(g:game)-[:on_map]->(m)
WITH u, m, achievement,
  length(filter(team in collect(p.team) where team = "RVSF")) AS countRvsf,
  length(filter(team in collect(p.team) where team = "CLA")) AS countCla
SET achievement.rvsfGames = countRvsf
SET achievement.claGames = countCla
SET achievement.rvsfRemain = CASE WHEN countRvsf >= achievement.rvsfTarget THEN 0 ELSE achievement.rvsfTarget - countRvsf END
SET achievement.claRemain = CASE WHEN countCla >= achievement.claTarget THEN 0 ELSE achievement.claTarget - countCla END
RETURN u, achievement;