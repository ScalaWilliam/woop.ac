MATCH (u: user), (m: good_map)
MATCH (u)-[link:ongoing]->(ach: map_completed_achievement)-[:of_map]->(m)
WHERE ach.rvsfGamesLeft = 0 AND ach.claGamesLeft = 0
WITH u, m, ach, link
DELETE link
MERGE (u)-[nl:completed]->(ach)
RETURN [u, nl, ach];