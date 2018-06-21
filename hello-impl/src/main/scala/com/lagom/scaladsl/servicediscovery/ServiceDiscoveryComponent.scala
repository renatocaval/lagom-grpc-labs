package com.lagom.scaladsl.servicediscovery

import akka.actor.ActorSystem

trait ServiceDiscoveryComponent {

  def actorSystem: ActorSystem
  
  lazy val serviceDiscovery: ServiceDiscovery = new ServiceDiscovery(actorSystem)
}
