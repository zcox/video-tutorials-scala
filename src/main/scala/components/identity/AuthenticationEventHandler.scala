package vt.components.identity

import messagedb._
import cats.Applicative
import cats.syntax.all._
import cats.effect._

//lock account on too many login failures. this could be a separate component, if multiple components can write events to same category stream (i.e. identity)

object AuthenticationEventHandler {

  def handle[F[_]: Temporal](mdb: MessageDb[F])(event: MessageDb.Read.Message): F[Unit] = 
    event.`type` match {
      case "UserLoginFailed" => 
        for {
          data <- event.decodeData[UserLoginFailed.Data].liftTo[F]
          metadata <- event.decodeMetadata[UserLoginFailed.Metadata].liftTo[F]
          () <- UserLoginFailedEventHandler.handle(mdb, UserLoginFailed(data.userId, data.reason, metadata.traceId))
        } yield ()
      case _ => Applicative[F].unit
    }
}
