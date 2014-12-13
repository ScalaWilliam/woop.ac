package acleague.ingesters

object GameMode {
  sealed trait Style {
    def isFrag: Boolean
    def isFlag: Boolean
  }
  sealed trait FragStyle extends Style {
    def isFrag = true
    def isFlag = false
  }
  sealed trait FlagStyle extends Style {
    def isFrag = false
    def isFlag = true
  }
  sealed abstract class GameMode(val name: String) extends Style
  case object HTF extends GameMode("hunt the flag") with FlagStyle
  case object CTF extends GameMode("ctf") with FlagStyle
  case object TOSOK extends GameMode("team one shot, one kill") with FragStyle
  case object TDM extends GameMode("team deathmatch") with FragStyle
  case object TS extends GameMode("team survivor") with FragStyle
  case object TKTF extends GameMode("team keep the flag") with FlagStyle
  val gamemodes: Seq[GameMode] = Seq(HTF, CTF, TOSOK, TDM, TKTF)
}