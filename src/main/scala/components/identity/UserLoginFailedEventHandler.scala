package vt.components.identity

import messagedb._
import cats.syntax.all._
import cats._
import cats.effect._
import java.util.UUID

object UserLoginFailedEventHandler {

  def loginFailureCount[F[_]: Temporal](mdb: MessageDb[F], userId: UUID): F[Int] = 
    mdb
      .getAllStreamMessages(s"authentication-$userId")
      .collect { case e if e.`type` == "UserLoginFailed" /*and reason is "Incorrect password" and within 1h and after unlocked....*/ => 1 }
      .compile
      .fold(0)(_ + _)

  def lockAccount(event: UserLoginFailed, loginFailures: Int): Option[AccountLocked] =
    if (loginFailures > 3)
      AccountLocked(event.userId, event.traceId).some
    else
      none

  def handle[F[_]: Temporal](mdb: MessageDb[F], event: UserLoginFailed): F[Unit] = 
    for {
      state <- loginFailureCount(mdb, event.userId)
      result = lockAccount(event, state)
      () <- result.fold(
        Applicative[F].unit
      )(e => 
        mdb.writeMessage_(e.toMessage)
      )
    } yield ()
}
