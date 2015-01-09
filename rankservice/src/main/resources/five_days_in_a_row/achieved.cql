MATCH (u)-[prog_link:in_progress]->(ach:five_days_in_a_row_achievement)
WHERE ach.completed = TRUE
DELETE prog_link
MERGE (u)-[link:achieved]->(ach)
RETURN [u, link, ach];