MATCH (u: user)
MERGE (u)-[:ongoing]->(achievement:fifty_flags_achievement)
SET achievement.target = 50
SET achievement.flags = 0
SET achievement.remaining = achievement.target - achievement.flags
WITH u, achievement
OPTIONAL MATCH (u)-[:completed]->(overflow_achievement:fifty_flags_achievement)
WITH u, achievement, COALESCE(sum(overflow_achievement.overflow), 0) AS carry_forward
SET achievement.flags = carry_forward
WITH u, achievement, carry_forward
MATCH (u)<-[:is_user]-(p:player), (p)<-[:has_player]-(g:game)
WHERE g.mode = "ctf" AND HAS(p.flags) AND NOT(()-[:completed]->(:fifty_flags_achievement)-[:includes]->(p))
WITH u, achievement, g, p, carry_forward
MERGE (achievement)-[:includes]->(p)
WITH SUM(p.flags) + carry_forward AS flags_so_far, achievement, u
SET achievement.flags = CASE WHEN flags_so_far >= achievement.target THEN achievement.target ELSE flags_so_far END
SET achievement.remaining = achievement.target - achievement.flags
SET achievement.overflow = CASE WHEN flags_so_far > achievement.target THEN flags_so_far - achievement.target ELSE NULL END
SET achievement.can_be_completed = achievement.flags = achievement.target
RETURN [u, achievement];