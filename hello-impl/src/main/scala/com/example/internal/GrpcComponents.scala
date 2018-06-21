package com.example.internal

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lagom.scaladsl.servicediscovery.ServiceDiscovery
import com.lightbend.lagom.scaladsl.api.ServiceLocator

import scala.concurrent.ExecutionContext

/**
  *
  */
trait GrpcComponents {

  def serviceDiscovery: ServiceDiscovery
  def actorSystem: ActorSystem
  def materializer: Materializer
  def executionContext: ExecutionContext

  val grpcChannelFactory: GrpcClientSettingsProvider =
    new PooledGrpcClientSettingsProvider(serviceDiscovery)(actorSystem, materializer, executionContext)
}
