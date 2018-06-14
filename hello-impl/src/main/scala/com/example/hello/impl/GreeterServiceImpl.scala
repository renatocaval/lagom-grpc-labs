package com.example.hello.impl

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import example.myapp.helloworld.grpc._

import scala.concurrent.Future

class GreeterServiceImpl(materializer: Materializer) extends GreeterService {

  private implicit val mat: Materializer = materializer

  override def sayHello(in: HelloRequest): Future[HelloReply] = {
    println(s"sayHello to ${in.name}")
    Future.successful(HelloReply(s"Hello, ${in.name}"))
  }

  override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] = ???

  override def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] = ???

  override def streamHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = ???
}