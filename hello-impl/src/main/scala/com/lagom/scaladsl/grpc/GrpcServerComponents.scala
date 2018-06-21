package com.lagom.scaladsl.grpc

import java.io.{ByteArrayOutputStream, InputStream}
import java.security.{KeyFactory, KeyStore, SecureRandom}
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}

trait GrpcServerComponents {


  def actorSystem: ActorSystem
  def materializer: Materializer
  def applicationLifecycle: ApplicationLifecycle


  // AkkaGrpcServer
  def grpcServerFor(serviceHandler: PartialFunction[HttpRequest, Future[HttpResponse]]): LagomGrpcServer = {
    val akkaGrpcServer =
      new EmbeddedAkkaGrpcServer(serviceHandler, actorSystem, materializer)

    // TODO: replace it by CoordinatedShutdown
    applicationLifecycle.addStopHook(() => akkaGrpcServer.shutdown)
    akkaGrpcServer
  }

  def lagomGrpcServer: LagomGrpcServer
}