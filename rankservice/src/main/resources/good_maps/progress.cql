MATCH (u: user)
WHERE NOT ((u)-[:completed]->(:maps_completed_achievement))
OPTIONAL MATCH (u)-[:ongoing]->(ongoing_sub_achievement: map_completed_achievement)
OPTIONAL MATCH (u)-[:completed]->(completed_sub_achievement: map_completed_achievement)
WITH u, collect(ongoing_sub_achievement)+ collect(completed_sub_achievement) as depends, length(collect(completed_sub_achievement)) AS completed, length(collect(ongoing_sub_achievement)) AS ongoing
WHERE length(depends) > 0
MERGE (u)-[:ongoing]->(achievement: maps_completed_achievement)
foreach(dep in depends | merge(achievement-[:depends_on]->(dep)))
SET achievement.target = ongoing + completed
SET achievement.progress = completed
SET achievement.remain = ongoing
RETURN u, achievement;