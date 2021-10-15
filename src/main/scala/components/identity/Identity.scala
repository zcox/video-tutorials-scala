package vt.components.identity

import messagedb._
import fs2.Stream
import cats.effect._
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Identity {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]  

  def component[F[_]: Async](mdb: MessageDb[F]): Stream[F, Unit] = {
    val identityCommandSubscription = mdb.subscribe(
      "identity:command",
      "components:identity:command",
      IdentityCommandHandler.handle(mdb),
    )

    val identityEventSubscription = mdb.subscribe(
      "identity",
      "components:identity",
      IdentityEventHandler.handle(mdb),
    )

    val authenticationEventSubscription = mdb.subscribe(
      "authentication",
      "components:identity:authentication",
      AuthenticationEventHandler.handle(mdb),
    )

    identityCommandSubscription
      .merge(identityEventSubscription)
      .merge(authenticationEventSubscription)
  }
}
