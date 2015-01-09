package acleague.achievements

/**
 * Created by William on 09/01/2015.
 */
object Achievements {

  /***
    * 500 frags. Give for every new 500 frags.
    * 50 flags. Give for every new 50 flags.
    * Map completed: playing twice on each side of each good map on two separate days. Give once.
    * All maps completed. Give once.
    * Play 5 days in a row. Give for every new 5 days.
    * Play 10 days in a row. Give for every new 10 days.
    * Play 10 games. Give for every new 10 games.
    * Play 10 hours. Give for every new 10 hours.
    * Play 5 games in a day. Give once a day at most. Allow every day.
    * Socialite: play against 10 other registered players at least twice. And then the 10 other players. And so on.
    */

  /**
   * Play 10 games
   *
   * MATCH (u: user) <-[:is_user]-(p:player)
   * WHERE NOT EXISTS(
   *   MATCH (achievement: 10_games_achievement)-[:included]->(p: player) RETURN achievement)
   * )
   * WHERE COUNT(p) >= 10
   * MERGE (u: user)-[:achieved]->(achievement: 10_games_achievement),
   *  (achievement)-[:included]->(p:player)
   * RETURN u
   */

  /** 500 frags
    *
    * MATCH (u: user)<-[:is_user]-(p: player),
    * WHERE NOT EXISTS (MATCH (achievement: 500_frags_achievement)-[:included]->(p: player) RETURN achievement)
    * MERGE (achievement: 500_frags_achievement{ frags: SUM(p.frags)
    * WHERE NOT EXISTS( MATCH (u)->[(a: 500_frags_achievement)-[
    *   (p) <- [:has_player] - (t:team),
    *   (t) <- [:has_team] - (g: game)
    *
    *
    *
    */
}
