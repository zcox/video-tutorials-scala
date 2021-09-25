package vt.application.authenticate

import cats._
import cats.syntax.all._
import cats.effect.Concurrent
import cats.data._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe._
import io.circe.generic.semiauto._
import org.http4s.circe._
import java.util.UUID
import messagedb._
import scala.util.control.NoStackTrace

object Routes {

  def validatePassword[F[_]](password: String, passwordHash: String)(implicit F: cats.ApplicativeError[F, Throwable]): F[Boolean] = {
    import com.github.t3hnar.bcrypt._
    password.isBcryptedSafeBounded(passwordHash).liftTo[F]
  }

  def writeLoggedInEvent[F[_]: Applicative](mdb: MessageDb[F], userId: UUID, traceId: UUID): F[Unit] = 
    mdb.writeMessage(
      UUID.randomUUID().toString,
      s"authentication-$userId",
      "UserLoggedIn",
      Json.obj(
        ("userId", Json.fromString(userId.toString)),
      ),
      Json.obj(
        ("traceId", Json.fromString(traceId.toString)),
        ("userId", Json.fromString(userId.toString)),
      ).some,
      none,
    ).void

  def writeLoginFailedEvent[F[_]: Applicative](mdb: MessageDb[F], userId: UUID, traceId: UUID): F[Unit] = 
    mdb.writeMessage(
      UUID.randomUUID().toString,
      s"authentication-$userId",
      "UserLoginFailed",
      Json.obj(
        ("userId", Json.fromString(userId.toString)),
        ("reason", Json.fromString("Incorrect password")),
      ),
      Json.obj(
        ("traceId", Json.fromString(traceId.toString)),
      ).some,
      none
    ).void

  case class LoginRequest(
    email: String,
    password: String,
  )
  object LoginRequest {
    implicit val decoder: Decoder[LoginRequest] = deriveDecoder[LoginRequest]
    implicit def entityDecoder[F[_]: Concurrent] = jsonOf[F, LoginRequest]
  }

  case object NotFoundError extends Exception with NoStackTrace
  case class CredentialMismatchError(userId: UUID) extends Exception with NoStackTrace

  def apply[F[_]: Concurrent](mdb: MessageDb[F], creds: vt.views.usercredentials.Read[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "log-in" =>
        val traceId = UUID.randomUUID()
        val result = for {
          request <- req.as[LoginRequest]
          user <- OptionT(creds.byEmail(request.email)).getOrElseF(ApplicativeError[F, Throwable].raiseError(NotFoundError))
          () <- validatePassword(request.password, user.passwordHash).ifM(Applicative[F].unit, ApplicativeError[F, Throwable].raiseError(CredentialMismatchError(user.userId)))
          () <- writeLoggedInEvent(mdb, user.userId, traceId)
          response <- Ok()
        } yield response
        result.recoverWith {
          case NotFoundError => 
            //should be Unauthorized, but that's more complex and I'm lazy
            Forbidden()
          case CredentialMismatchError(userId) => 
            writeLoginFailedEvent(mdb, userId, traceId) *>
            Forbidden()
        }
    }
  }
}

