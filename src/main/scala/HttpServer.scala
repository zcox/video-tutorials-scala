package vt

import cats.effect.{Async, ExitCode}
import fs2.Stream
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global

object HttpServer {

  def stream[F[_]: Async](httpApp: HttpApp[F]): Stream[F, ExitCode] =
    BlazeServerBuilder[F](global)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(Logger.httpApp(true, true)(httpApp))
      .serve
}
