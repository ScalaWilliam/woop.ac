package controllers

import play.api.mvc.{Action, Result, AnyContent, Request}
import plugins.RegisteredUserManager
import plugins.RegisteredUserManager.{GoogleEmailAddress, RegisteredSession, SessionState}
import scala.concurrent.Future
import play.api.mvc.Results._

trait SysActions {
  def registeredUserManager: RegisteredUserManager

  def stated[V](f: Request[AnyContent] => SessionState => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      registeredUserManager.getSessionState.flatMap { implicit session =>
        f(request)(session)
      }
  }

  def statedSync[V](f: Request[AnyContent] => SessionState => Result): Action[AnyContent] =
    stated { a => b =>
      Future{f(a)(b)}
    }
  def registeredSync[V](f: Request[AnyContent] => RegisteredSession => Result): Action[AnyContent] =
    registered { a => b =>
      Future{f(a)(b)}
    }

  def registered[V](f: Request[AnyContent] => RegisteredSession => Future[Result]): Action[AnyContent] =
    stated { implicit request => {
      case SessionState(Some(sessionId), Some(GoogleEmailAddress(email)), Some(profile)) =>
        f(request)(RegisteredSession(sessionId, profile))
      case other =>
        Future {
          SeeOther(controllers.routes.Accounting.createProfile().url)
        }
    }
    }

  def using[T <: { def close(): Unit }, V](a: => T)(f: T=> V):V  = {
    val i = a
    try f(i)
    finally i.close()
  }

}
