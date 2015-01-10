MATCH (u: user)-[link:ongoing]->(maps_ach: maps_completed_achievement)
WHERE maps_ach.remain = 0
DELETE link
MERGE (u)-[newlink:completed]->(maps_ach)
RETURN u, newlink, maps_ach;