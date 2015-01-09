MATCH (u: user), (m: good_map)
WHERE NOT ((u)-[:completed]->(:map_completed_achievement)-[:of_map]->(m))
OPTIONAL MATCH (u)<-[:is_user]-(p:player), (p)<-[:has_player]-(g:game), (g)-[:on_map]->m
WITH u, m, collect(p.team) AS teams
WITH u, m, length(filter(x IN teams WHERE x = "RVSF")) AS cntRvsf,
     length(filter(x IN teams WHERE x = "CLA")) AS cntCla
WITH u, m, cntRvsf, cntCla, 2 - cntRvsf AS leftRvsf, 2 - cntCla AS leftCla, (cntRvsf = 2) AND (cntCla = 2) AS completedB
MERGE (u)-[:ongoing]->(nach: map_completed_achievement)
MERGE (nach)-[:of_map]->(m)
SET nach.rvsfGames = cntRvsf
SET nach.claGames = cntCla
SET nach.rvsfGamesLeft = leftRvsf
SET nach.claGamesLeft = leftCla
SET nach.u = u.name
RETURN u, nach;