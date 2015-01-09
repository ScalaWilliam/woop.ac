MATCH (u: user)-[link:ongoing]->(achievement:fifty_games)
WHERE achievement.target > 0 AND achievement.remain = 0
DELETE link
MERGE (u)-[:completed]->(achievement)
RETURN u, achievement