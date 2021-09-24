package vt.views.usercredentials

import java.util.UUID
import cats._
import cats.syntax.all._
import cats.effect.MonadCancel
import skunk._
import skunk.implicits._
import skunk.codec.all._
import cats.effect.Resource

trait Write[F[_]] {
  def createUserCredential(id: UUID, email: String, passwordHash: String): F[Unit]
}

object Write {

  object CreateUserCredential {
    val command: Command[UUID ~ String ~ String] = 
      sql"""
        INSERT INTO
          user_credentials (user_id, email, password_hash)
        VALUES
          ($uuid, $text, $text)
        ON CONFLICT DO NOTHING
      """.command
  }

  def fromSession[F[_]: Functor](session: Session[F]): Resource[F, Write[F]] = 
    for {
      createUserCredentialCommand <- session.prepare(CreateUserCredential.command)
    } yield new Write[F] {
      override def createUserCredential(id: UUID, email: String, passwordHash: String): F[Unit] =
        createUserCredentialCommand.execute(id ~ email ~ passwordHash).void
    }

  def useEachTime[F[_]](r: Resource[F, Write[F]])(implicit F: MonadCancel[F, Throwable]): Write[F] = 
    new Write[F] {
      override def createUserCredential(id: UUID, email: String, passwordHash: String): F[Unit] =
        r.use(_.createUserCredential(id, email, passwordHash))
    }

  def useEachTime2[F[_]](r: Resource[F, Session[F]])(implicit F: MonadCancel[F, Throwable]): Write[F] = 
    useEachTime[F](r.flatMap(fromSession[F]))

}
