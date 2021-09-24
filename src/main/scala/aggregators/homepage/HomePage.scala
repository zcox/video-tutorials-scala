package vt.aggregators.homepage

import messagedb._
import cats.Applicative
import cats.effect._
import cats.effect.std.Console
import fs2.Stream

object HomePage {

  def logViewings[F[_]: Temporal: Console](mdb: MessageDb[F]): Stream[F, Unit] =
    mdb.subscribe(
      "viewing", 
      "aggregators:log-viewings", 
      Console[F].println(_),
    )

  def aggregator[F[_]: Temporal](mdb: MessageDb[F], view: vt.views.homepage.Write[F]): Stream[F, Unit] = 
    mdb.subscribe(
      "viewing",
      "aggregators:home-page",
      handle[F](view),
    )

  def handle[F[_]: Applicative](view: vt.views.homepage.Write[F])(message: MessageDb.Read.Message): F[Unit] = 
    message.`type` match {
      case "VideoViewed" => view.incrementVideosWatched(message.globalPosition)
      case _ => Applicative[F].unit
    }

}
