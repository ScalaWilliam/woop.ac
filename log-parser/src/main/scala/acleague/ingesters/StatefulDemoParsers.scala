package acleague.ingesters

sealed trait DemoCollector {
  def next(input: String): DemoCollector
}
object DemoCollector {
  def empty: DemoCollector = NoDemosCollected
}
case object NoDemosCollected extends DemoCollector {
  def next(input: String) = input match {
    case DemoRecorded(demo) => DemoRecordedCollected(demo)
    case _ => NoDemosCollected
  }
}
case class DemoRecordedCollected(demo: DemoRecorded) extends DemoCollector {
  def next(input: String) = input match {
    case DemoWritten(demoWritten) => DemoWrittenCollected(demo, demoWritten)
    case _ => DemoNotWrittenCollected(demo, input)
  }
}
case class DemoNotWrittenCollected(demo: DemoRecorded, followingLine: String) extends DemoCollector {
  def next(input: String) = NoDemosCollected.next(input)
}
case class DemoWrittenCollected(demo: DemoRecorded, demoWritten: DemoWritten) extends DemoCollector {
  def next(input: String) = NoDemosCollected.next(input)
}