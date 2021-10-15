package vt.components.identity

import messagedb._
import cats.Applicative
import cats.syntax.all._
import cats.effect._
import org.typelevel.log4cats.Logger

object IdentityEventHandler {

  def handle[F[_]: Temporal: Logger](mdb: MessageDb[F])(event: MessageDb.Read.Message): F[Unit] = 
    event.`type` match {
      case "Registered" => 
        for {
          data <- event.decodeData[Registered.Data].liftTo[F]
          metadata <- event.decodeMetadata[Registered.Metadata].liftTo[F]
          () <- RegisteredEventHandler.handle(mdb, Registered(data.userId, data.email, data.passwordHash, metadata.traceId))
        } yield ()
      case _ => Applicative[F].unit
    }
}
