package acleague.ranker.achievements

object MapAchievements {
  case class AcMap(mode: String, name: String)
  val captureMaster = {
    val ctf = Set("ac_sunset", "ac_shine", "ac_power", "ac_mines", "ac_ingress", "ac_gothic", "ac_elevation", "ac_desert3", "ac_depot")
    val tosok = Set("ac_desert", "ac_arctic")
    val tdm = Set("ac_desert", "ac_arctic", "ac_complex")
    ctf.map(a => AcMap(mode = "ctf", name = a))
  }
}
