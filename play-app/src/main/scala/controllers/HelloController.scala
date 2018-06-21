package controllers

import example.myapp.helloworld.grpc.{ GreeterService, HelloRequest }
import play.api.mvc.{ AbstractController, ControllerComponents, Results }

import scala.concurrent.{ ExecutionContext, Future }
class HelloController(client: GreeterService, controllerComponents: ControllerComponents)
                     (implicit ec: ExecutionContext)
  extends AbstractController(controllerComponents) {

  def hello(name: String) = Action { implicit req =>
    Results.Ok(s"Hello $name")
  }

  def helloGrpc(name: String) = Action.async { implicit req =>
    client.sayHello(HelloRequest(name)).map(rp => Results.Ok( s"Received [${rp.message}] via gRPC."))
  }
}
