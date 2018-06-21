package com.example.hello.api

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

trait HelloService extends Service {

  /**
    * Example: curl http://localhost:9000/api/hello/Alice
    */
  def hello(name: String): ServiceCall[NotUsed, String]

  def helloGrpc(name: String): ServiceCall[NotUsed, String]

  override final def descriptor = {
    import Service._
    named("hello")
      .withCalls(
        pathCall("/lagom/hello/:name", hello _),
        pathCall("/lagom/hello/:name/grpc", helloGrpc _)
      )
      .withAutoAcl(true)
      
  }
}
