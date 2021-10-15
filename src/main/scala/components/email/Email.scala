package vt.components.email

import cats.effect._
import fs2.Stream
import messagedb._
import org.typelevel.log4cats.slf4j.Slf4jLogger

object EmailComponent {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  def component[F[_]: Async](mdb: MessageDb[F], emailSender: EmailSender[F]): Stream[F, Unit] = 
    mdb.subscribe(
      "sendEmail:command",
      "components:send-email",
      SendEmailCommandHandler.handle[F](mdb, emailSender),
    )
}
