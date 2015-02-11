import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp
object SprayBasicApp extends App with SimpleRoutingApp {

  implicit val as = ActorSystem("main")
  import akka.actor.ActorDSL._

  startServer(interface = "0.0.0.0", port = 8765) {
    path("") {
      complete{"hello"}
    } ~ path("server") {
      put {

        complete{println("POST CALLED"); "Post returned"}
      } ~ delete {
        println("DELETE CALLED")
        complete("Delete returned")
      }
    }
  }

}