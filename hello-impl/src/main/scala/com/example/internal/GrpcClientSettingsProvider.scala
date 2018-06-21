package com.example.internal

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import com.lagom.scaladsl.servicediscovery.ServiceDiscovery
import com.lightbend.lagom.scaladsl.api.ServiceLocator

import scala.concurrent.{ExecutionContext, Future}

trait GrpcClientSettingsProvider {
  /**
    * Obtains a Channel for the `serviceName` and invokes `block` using a client built with that channel.
    * When completing `block` the channel may be released or destroyed depending on the implementation.
    */
  def withSettings[Client](serviceName: String, portName: String)(block: GrpcClientSettings => Client): Future[Client]
}

class PooledGrpcClientSettingsProvider(serviceDiscovery: ServiceDiscovery)(implicit sys: ActorSystem, mat: Materializer, ex: ExecutionContext) extends GrpcClientSettingsProvider {

  type SettingsFactory = (String, String) => Future[GrpcClientSettings]

  private val settingsFactory: SettingsFactory = (name, portName) =>
    serviceDiscovery
      .lookupOne(name, portName)
      .flatMap { maybeUri =>
        maybeUri
          .map { uri =>
            println(s"URI: $uri, host: ${uri.getHost}, port: ${uri.getPort}")
            GrpcClientSettings(uri.getHost, uri.getPort)
              .withOverrideAuthority("foo.test.google.fr")
              .withTrustedCaCertificate("ca.pem")
          }
          .map(Future.successful)
          .getOrElse(Future.failed(new RuntimeException(s"Service $name not found.")))
      }

  def withSettings[Client](serviceName: String, portName: String)(block: GrpcClientSettings => Client): Future[Client] =
    settingsFactory(serviceName, portName).map(block(_))
}
