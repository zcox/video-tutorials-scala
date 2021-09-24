package vt.application.register

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

object Routes {

  //TODO
  def validateEmail(email: String): Either[String, Unit] = {
    val _ = email
    Right(())
  }

  //TODO
  def validatePassword(password: String): Either[String, Unit] = {
    val _ = password
    Right(())
  }

  def hashPassword[F[_]](password: String)(implicit F: cats.ApplicativeError[F, Throwable]): F[String] = {
    import com.github.t3hnar.bcrypt._
    password.bcryptSafeBounded.liftTo[F]
  }

  def writeRegisterCommand[F[_]: cats.Applicative](mdb: MessageDb[F], userId: UUID, email: String, hashedPassword: String, traceId: UUID): F[Unit] = 
    mdb.writeMessage(
      UUID.randomUUID().toString,
      s"identity:command-$userId",
      "Register",
      Json.obj(
        ("userId", Json.fromString(userId.toString)),
        ("email", Json.fromString(email)),
        ("passwordHash", Json.fromString(hashedPassword)),
      ),
      Json.obj(
        ("traceId", Json.fromString(traceId.toString)),
        ("userId", Json.fromString(userId.toString)),
      ).some,
      none,
    ).void

  case class RegisterRequest(
    userId: UUID,
    email: String,
    password: String,
  )
  object RegisterRequest {
    implicit val decoder: Decoder[RegisterRequest] = deriveDecoder[RegisterRequest]
    implicit def entityDecoder[F[_]: Concurrent] = jsonOf[F, RegisterRequest]
  }

  def apply[F[_]: Concurrent](mdb: MessageDb[F], creds: vt.views.usercredentials.Read[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "register" =>
        val traceId = UUID.randomUUID()
        val result = for {
          request <- EitherT.liftF[F, String, RegisterRequest](req.as[RegisterRequest])
          () <- EitherT.fromEither[F](validateEmail(request.email))
          () <- EitherT.fromEither[F](validatePassword(request.password))
          () <- OptionT(creds.byEmail(request.email)).toLeft[Unit](()).leftMap((_: vt.views.usercredentials.Read.UserCredential) => s"User already exists with email ${request.email}")
          hashedPassword <- EitherT.liftF[F, String, String](hashPassword(request.password))
          () <- EitherT.liftF[F, String, Unit](writeRegisterCommand(mdb, request.userId, request.email, hashedPassword, traceId))
        } yield ()
        result.foldF(e => BadRequest(e), _ => Ok())
    }
  }
}
