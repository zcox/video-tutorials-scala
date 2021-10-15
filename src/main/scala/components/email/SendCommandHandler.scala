package vt.components.email

import cats.syntax.all._
import cats.effect._
import java.util.UUID
import messagedb._
import org.typelevel.log4cats.Logger

object SendCommandHandler {

  def alreadySent[F[_]: Temporal](mdb: MessageDb[F], emailId: UUID): F[Option[Sent]] =
    mdb
      .getAllStreamMessages(s"sendEmail-$emailId")
      .find(_.`type` == "Sent")
      .evalMap(m => Sent.from[F](m))
      .compile
      .last

  def send[F[_]: Temporal](emailSender: EmailSender[F], command: Send, alreadySent: Option[Sent]): F[Result] =
    alreadySent.fold(
      emailSender.sendEmail(Email.from(command))
        .map[Result](_ => Sent.from(command))
        .recover {
          case EmailSendError(reason) => Failed.from(command, reason)
        }
    )(e => (AlreadySentError(e): Result).pure[F])

  def handle[F[_]: Temporal: Logger](mdb: MessageDb[F], emailSender: EmailSender[F], command: Send): F[Unit] = 
    for {
      state <- alreadySent(mdb, command.emailId)
      result <- send(emailSender, command, state)
      () <- result match {
        case AlreadySentError(e) => Logger[F].warn(s"Ignoring command $command due to email already sent: $e")
        case s: Sent => mdb.writeMessage_(s.toMessage)
        case f: Failed => mdb.writeMessage_(f.toMessage)
      }
    } yield ()
}
