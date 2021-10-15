package vt.components.identity

import messagedb._
import cats.syntax.all._
import cats._
import cats.effect._
import org.typelevel.log4cats.Logger

object IdentityCommandHandler {

  def handle[F[_]: Temporal: Logger](mdb: MessageDb[F])(command: MessageDb.Read.Message): F[Unit] = 
    command.`type` match {
      case "Register" => 
        for {
          data <- command.decodeData[Register.Data].liftTo[F]
          metadata <- command.decodeMetadata[Register.Metadata].liftTo[F]
          () <- RegisterCommandHandler.handle(mdb, Register(data.userId, data.email, data.passwordHash, metadata.traceId))
        } yield ()
      case _ => Applicative[F].unit
    }
}
