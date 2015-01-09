// we will set previous complete achievement overflows to 0 and put them into our own
MATCH (u: user)-[ongoing_link:ongoing]->(about_to_complete:fifty_flags_achievement)
WHERE about_to_complete.can_be_completed = TRUE
DELETE ongoing_link
WITH u, about_to_complete
OPTIONAL MATCH (u)-[:completed]->(completed_achievements:fifty_flags_achievement)
SET completed_achievements.overflow = NULL
SET completed_achievements.can_be_completed = FALSE
MERGE (u)-[:completed]->(about_to_complete)
MERGE (u)-[new_ongoing_link:ongoing]->(new_ongoing_achievement:fifty_flags_achievement)
SET new_ongoing_achievement.target = about_to_complete.target
SET new_ongoing_achievement.flags = COALESCE(about_to_complete.overflow, 0)
SET new_ongoing_achievement.remaining = new_ongoing_achievement.target - new_ongoing_achievement.flags
SET completed_achievements.can_be_completed = FALSE
RETURN [u, new_ongoing_achievement];