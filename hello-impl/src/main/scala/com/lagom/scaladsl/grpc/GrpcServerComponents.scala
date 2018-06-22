package com.lagom.scaladsl.grpc

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.server.LagomServer
import play.api.inject.ApplicationLifecycle
import play.api.mvc.Handler
import play.api.routing.Router.Routes
import play.api.routing.{Router, SimpleRouter}

import scala.concurrent.Future

trait GrpcServerComponents {


  def actorSystem: ActorSystem
  def materializer: Materializer
  def applicationLifecycle: ApplicationLifecycle

  def composeRouter(lagomServer: LagomServer, serviceHandler: PartialFunction[HttpRequest, Future[HttpResponse]]): Router = {
    val grpcRouter = routerFor(serviceHandler)

    lagomServer.router.routes.orElse(grpcRouter.routes)

    new SimpleRouter {
      override def routes: Routes = lagomServer.router.routes.orElse(grpcRouter.routes)
    }
  }

  private def routerFor(serviceHandler: PartialFunction[HttpRequest, Future[HttpResponse]]): Router ={
    new Router {

      val handler = new AkkaHttpHandler {
        override def apply(request: HttpRequest): Future[HttpResponse] = serviceHandler(request)
      }

      override def routes: Routes = { case _ ⇒ handler }

      override def documentation: Seq[(String, String, String)] = Seq.empty

      override def withPrefix(prefix: String): Router = this
    }
  }

  trait AkkaHttpHandler extends (HttpRequest => Future[HttpResponse]) with Handler

  object AkkaHttpHandler {
    def apply(handler: HttpRequest => Future[HttpResponse]): AkkaHttpHandler = new AkkaHttpHandler {
      def apply(request: HttpRequest): Future[HttpResponse] = handler(request)
    }
  }
}