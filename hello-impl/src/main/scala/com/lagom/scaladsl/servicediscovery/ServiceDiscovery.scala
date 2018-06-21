package com.lagom.scaladsl.servicediscovery

import java.net.URI

import akka.actor.ActorSystem
import com.lightbend.rp.servicediscovery.scaladsl.{Service, ServiceLocator}
import scala.collection.immutable
import scala.concurrent.Future

class ServiceDiscovery(actorSystem: ActorSystem) {

  def lookupOne(name: String, endpoint: String): Future[Option[URI]] ={

    implicit val as = actorSystem
    implicit val exec = actorSystem.dispatcher

    for {
      portName <- ServiceLocator.lookupOne(name, endpoint)
      result <- portName match {
        case None => ServiceLocator.lookupOne(name)
        case Some(r) => Future.successful(Some(r))
      }
    } yield result.map(_.uri)
  }

}
