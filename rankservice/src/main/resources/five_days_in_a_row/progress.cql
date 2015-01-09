// incorrectly implemented. It will allow for overlap, so actually sometimes 4 days.
MATCH (u:user)
WITH u
MATCH (u)<-[:is_user]-(p:player), (g:game)-[:has_player]->(p)
WHERE NOT (u)-[:achieved]->(:five_days_in_a_row_achievement)-[:includes]->(p)
WITH COUNT(DISTINCT g.date) AS dates, u
MERGE (u)-[:in_progress]->(ach:five_days_in_a_row_achievement)
SET ach.completed = dates >= 5
MERGE (ach)-[:includes]->(p)
RETURN [u, ach];