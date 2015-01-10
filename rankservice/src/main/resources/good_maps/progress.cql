MATCH (u: user)
WHERE NOT ((u)-[:completed]->(:maps_completed_achievement))
OPTIONAL MATCH (u)-[link]->(:map_completed_achievement)
WITH u, collect(link) AS links
WHERE length(links) > 0
OPTIONAL MATCH (u)-->(sub_achievement: map_completed_achievement)
MERGE (u)-[:ongoing]->(achievement: maps_completed_achievement)-[:depends_on]->(sub_achievement)
WITH u, achievement, links
SET achievement.target = length(links)
SET achievement.remain = length(filter(sa in links where type(sa) = "ongoing"))
SET achievement.progress = length(filter(sa in links where type(sa) = "completed"))
RETURN u, achievement;