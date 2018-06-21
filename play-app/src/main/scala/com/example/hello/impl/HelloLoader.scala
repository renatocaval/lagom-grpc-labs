package com.example.hello.impl

import akka.NotUsed
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.rp.servicediscovery.lagom.scaladsl.LagomServiceLocatorComponents
import com.softwaremill.macwire._
import controllers.HelloController
import example.myapp.helloworld.grpc.{GreeterService, HelloReply, HelloRequest}
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Mode}
import play.filters.HttpFiltersComponents
import router.Routes

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class HelloLoader extends ApplicationLoader {
  def load(context: Context) =
    context.environment.mode match {
    case Mode.Dev =>
      (new HelloApplication(context) with LagomDevModeComponents).application
    case _ =>
      (new HelloApplication(context) with LagomServiceLocatorComponents).application
  }
  
}

abstract class HelloApplication(context: Context)
  extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with HttpFiltersComponents
    with LagomConfigComponent
    with LagomServiceClientComponents {

  lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "web-gateway",
    Map(
      "web-gateway" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }

  val remotePort : Int = 8080
  val client: GreeterService = wire[GreeterServiceClient]

  val hello: HelloController = wire[HelloController]

}

class GreeterServiceClient(remotePort:Int)(mat: Materializer, ex: ExecutionContext) extends GreeterService {
  private implicit val materializer = mat
  private implicit val executionContext = ex

  val settings = GrpcClientSettings("10.108.169.215", remotePort)
    .withOverrideAuthority("foo.test.google.fr")
    .withTrustedCaCertificate("ca.pem")

  override def sayHello(in: HelloRequest): Future[HelloReply] =
    example.myapp.helloworld.grpc.GreeterServiceClient(settings).sayHello(in)

  override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] = ???

  override def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] = ???

  override def streamHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = ???
}

