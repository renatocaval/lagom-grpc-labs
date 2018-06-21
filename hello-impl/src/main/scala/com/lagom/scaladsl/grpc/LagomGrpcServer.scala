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

import scala.concurrent.{ExecutionContext, Future}

trait LagomGrpcServer


/**
  * Internal API
  */
private [lagom] class EmbeddedAkkaGrpcServer(serviceHandler: PartialFunction[HttpRequest, Future[HttpResponse]],
                                             actorSystem: ActorSystem,
                                             materializer: Materializer) extends LagomGrpcServer {

  implicit val sys = actorSystem
  implicit val mat = materializer
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val service: HttpRequest => Future[HttpResponse] =
    serviceHandler
      .orElse { case _ => Future.successful(HttpResponse(StatusCodes.NotFound)) }

  val config = actorSystem.settings.config
  val host = config.getString("akka-grpc.server.http.address")
  val remotePort = config.getInt("akka-grpc.server.http.port")
  private val eventualBinding: Future[Http.ServerBinding] = Http().bindAndHandleAsync(
    service,
    interface = host,
    port = remotePort,
    connectionContext = serverHttpContext()
  )
  eventualBinding.foreach { binding =>
    println(s"gRPC server bound to: ${binding.localAddress}")
  }

  // TODO: replace it by CoordinatedShutdown
  def shutdown =
    eventualBinding.flatMap {
      bind =>
        bind.unbind()
    }

  private def serverHttpContext(): HttpsConnectionContext = {
    // FIXME how would end users do this? TestUtils.loadCert? issue #89
    val keyEncoded = read(getClass.getResourceAsStream("/certs/server1.key"))
      .replace("-----BEGIN PRIVATE KEY-----\n", "")
      .replace("-----END PRIVATE KEY-----\n", "")
      .replace("\n", "")

    val decodedKey = Base64.getDecoder.decode(keyEncoded)

    val spec = new PKCS8EncodedKeySpec(decodedKey)

    val kf = KeyFactory.getInstance("RSA")
    val privateKey = kf.generatePrivate(spec)

    val fact = CertificateFactory.getInstance("X.509")
    //    val is = new FileInputStream(TestUtils.loadCert("server1.pem"))
    val cer = fact.generateCertificate(getClass.getResourceAsStream("/certs/server1.pem"))

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry("private", privateKey, Array.empty, Array(cer))

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

    new HttpsConnectionContext(context)
  }

  private def read(in: InputStream): String = {
    val bytes: Array[Byte] = {
      val baos = new ByteArrayOutputStream(math.max(64, in.available()))
      val buffer = Array.ofDim[Byte](32 * 1024)

      var bytesRead = in.read(buffer)
      while (bytesRead >= 0) {
        baos.write(buffer, 0, bytesRead)
        bytesRead = in.read(buffer)
      }
      baos.toByteArray
    }
    new String(bytes, "UTF-8")
  }

}
