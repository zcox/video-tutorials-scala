package vt.components.identity

import messagedb._
import cats.syntax.all._
import cats.effect._
import java.util.UUID
import org.typelevel.log4cats.Logger

object RegisterCommandHandler {

  def identityExists[F[_]: Temporal](mdb: MessageDb[F], userId: UUID): F[Boolean] = 
    mdb.isStreamEmpty(s"identity-$userId").map(!_)

  def register(command: Register, identityExists: Boolean): Either[AlreadyRegisteredError, Registered] = 
    if (identityExists)
      AlreadyRegisteredError().asLeft
    else
      Registered(command.userId, command.email, command.passwordHash, command.traceId).asRight

  def handle[F[_]: Temporal: Logger](mdb: MessageDb[F], command: Register): F[Unit] = 
    for {
      state <- identityExists(mdb, command.userId)
      result = register(command, state)
      () <- result.fold(
        _ => Logger[F].warn(s"Identity already registered, ignoring command: $command"),
        event => mdb.writeMessage_(event.toMessage),
      )
    } yield ()
}
