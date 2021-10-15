package vt.components.email

import cats._
import cats.syntax.all._
import cats.effect._
import messagedb._
import org.typelevel.log4cats.Logger

object SendEmailCommandHandler {

  def handle[F[_]: Async: Logger](mdb: MessageDb[F], emailSender: EmailSender[F])(command: MessageDb.Read.Message): F[Unit] = 
    command.`type` match {
      case "Send" => 
        for {
          data <- command.decodeData[Send.Data].liftTo[F]
          metadata <- command.decodeMetadata[Send.Metadata].liftTo[F]
          () <- SendCommandHandler.handle(mdb, emailSender, Send.from(data, metadata))
        } yield ()
      case _ => Applicative[F].unit
    }
}
