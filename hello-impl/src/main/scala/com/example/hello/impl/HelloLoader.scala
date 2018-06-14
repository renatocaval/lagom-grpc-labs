package com.example.hello.impl

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.example.hello.api.HelloService
import com.example.internal.{GrpcClientSettingsProvider, GrpcComponents}
import com.lagom.scaladsl.grpc.GrpcServerComponents
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import example.myapp.helloworld.grpc.{GreeterService, GreeterServiceHandler, HelloReply, HelloRequest}
import play.api.libs.ws.ahc.AhcWSComponents
import scala.concurrent.{ExecutionContext, Future}

class HelloLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloService])
}

abstract class HelloApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with GrpcComponents
    with GrpcServerComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[HelloService](wire[HelloServiceImpl])

  val client: GreeterService = wire[GreeterServiceClient]
  val remotePort:Int = 8080
  val lagomGrpcServer = grpcServerFor(GreeterServiceHandler(new GreeterServiceImpl(materializer)), remotePort)

}

// Implementation note: mat and ex are not implicit so `macwire` can set them
class GreeterServiceClient(grpcClientSettingsProvider: GrpcClientSettingsProvider)(mat: Materializer, ex: ExecutionContext) extends GreeterService {
  private implicit val materializer = mat
  private implicit val executionContext = ex

  override def sayHello(in: HelloRequest): Future[HelloReply] =
    grpcClientSettingsProvider.withSettings(GreeterService.name) { settings =>
      example.myapp.helloworld.grpc.GreeterServiceClient(settings).sayHello(in)
    }

  override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] = ???
  override def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] = ???
  override def streamHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = ???
}