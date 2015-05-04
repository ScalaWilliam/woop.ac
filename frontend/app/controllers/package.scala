import scala.concurrent.ExecutionContext

package object controllers {
  implicit def ctx: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
}