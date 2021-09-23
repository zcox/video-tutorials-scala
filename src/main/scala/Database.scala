package vt

import cats.effect._
import skunk._
import natchez.Trace.Implicits.noop
import fs2.io.net.Network
import cats.effect.std.Console

object Database {

  def messageDbSessionResource[F[_]: Concurrent: Network: Console]: Resource[F, Resource[F, Session[F]]] =
    Session.pooled(
      host = "localhost",
      port = 5432,
      user = "message_store",
      database = "message_store",
      password = Some("message_store"),
      //no idea how many sessions to use...
      max = 4,
    )

  def viewSessionResource[F[_]: Concurrent: Network: Console]: Resource[F, Resource[F, Session[F]]] =
    Session.pooled(
      host = "localhost",
      port = 5433,
      user = "postgres",
      database = "postgres",
      password = Some("postgres"),
      //no idea how many sessions to use...
      max = 4,
    )

}
