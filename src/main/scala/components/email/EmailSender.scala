package vt.components.email

import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait EmailSender[F[_]] {
  def sendEmail(email: Email): F[Unit]
}

object EmailSender {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  def fake[F[_]: Sync](): EmailSender[F] = 
    new EmailSender[F] {
      //TODO for fun, fail with EmailSendError some of the time
      override def sendEmail(email: Email): F[Unit] = 
        Logger[F].info(s"Sent email: $email")
    }
}
