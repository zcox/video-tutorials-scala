package vt.views.usercredentials

import java.util.UUID
import cats.effect.MonadCancel
import skunk._
import skunk.implicits._
import skunk.codec.all._
import cats.effect.Resource

trait Read[F[_]] {
  def byEmail(email: String): F[Option[Read.UserCredential]]
}

object Read {

  case class UserCredential(
    userId: UUID,
    email: String,
    passwordHash: String,
  )

  object UserCredential {
    val codec = uuid ~ text ~ text
    val decoder = codec.gmap[UserCredential]
  }

  object ByEmail {
    val query: Query[String, UserCredential] = 
      sql"""
      SELECT user_id, email, password_hash FROM user_credentials WHERE email = $text
      """.query(UserCredential.decoder)
  }

  def fromSession[F[_]](session: Session[F]): Resource[F, Read[F]] = 
    for {
      byEmailQuery <- session.prepare(ByEmail.query)
    } yield new Read[F] {
      override def byEmail(email: String): F[Option[Read.UserCredential]] = 
        byEmailQuery.option(email)
    }

  def useEachTime[F[_]](r: Resource[F, Read[F]])(implicit F: MonadCancel[F, Throwable]): Read[F] = 
    new Read[F] {
      override def byEmail(email: String): F[Option[Read.UserCredential]] = 
        r.use(_.byEmail(email))
    }

  def useEachTime2[F[_]](r: Resource[F, Session[F]])(implicit F: MonadCancel[F, Throwable]): Read[F] = 
    useEachTime[F](r.flatMap(fromSession[F]))

}
