package com.example.hello.impl

import com.example.hello.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import example.myapp.helloworld.grpc.{ GreeterService, HelloRequest }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
  * Implementation of the HelloService.
  */
class HelloServiceImpl(greeter: GreeterService)(implicit ex:ExecutionContext) extends HelloService {

  override def hello(name: String) = ServiceCall { _ =>
    Future.successful(s"Hello $name")
  }

  override def helloGrpc(name: String) = ServiceCall { _ =>
    greeter.sayHello(HelloRequest(s"Hello $name")).map(_.message).map(s => s"Received [$s] via gRPC.")
  }

}
