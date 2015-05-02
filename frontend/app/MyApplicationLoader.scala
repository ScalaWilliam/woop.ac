import play.api._
import play.api.ApplicationLoader.Context
import play.api.libs.ws.WS
import play.api.routing.Router
import plugins.{DataSourcePlugin, BasexProviderPlugin}
//
//class MyApplicationLoader extends ApplicationLoader {
//  def load(context: Context) = {
//    new MyComponents(context).application
//  }
//}
//
//class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) {
//  lazy val basex = new BasexProviderPlugin(application, WS.client(application))
//  lazy val dsp = new DataSourcePlugin(basex, applicationLifecycle)
//  override def router: Router = Router.load(environment, configuration)
//}
