package vt.application

import java.util.UUID
import cats._
import cats.data._
import org.http4s._
import scala.util.Try
import org.typelevel.ci.CIString

object DumbAuth {

  /*
  We don't need a real auth system for this example application.
  When testing via curl, just include headers to simulate auth.
  */

  val UserIdHeader = CIString("X-User-Id")

  def impl[F[_]: Applicative]: Kleisli[OptionT[F, *], Request[F], User] = 
    Kleisli { request => 
      OptionT.fromOption[F](
        for {
          userId <- request.headers.get(UserIdHeader).flatMap(h => Try(UUID.fromString(h.head.value)).toOption)
        } yield User(userId)
      )
    }
}
