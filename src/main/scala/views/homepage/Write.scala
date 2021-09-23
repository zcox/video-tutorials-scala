package vt.views.homepage

import cats._
import cats.syntax.all._
import cats.effect.MonadCancel
import skunk._
import skunk.implicits._
import skunk.codec.all._
import cats.effect.Resource

trait Write[F[_]] {
  def incrementVideosWatched(globalPosition: Long): F[Unit]
}

object Write {

  object IncrementVideosWatched {
    val command: Command[Long ~ Long] =
      sql"""
        UPDATE
          pages
        SET
          page_data = jsonb_set(
            jsonb_set(
              page_data,
              '{videosWatched}',
              ((page_data ->> 'videosWatched')::int + 1)::text::jsonb
            ),
            '{lastViewProcessed}',
            $int8::text::jsonb
          )
        WHERE
          page_name = 'home' AND
          (page_data->>'lastViewProcessed')::int < $int8
      """.command
  }

  def fromSession[F[_]: Functor](session: Session[F]): Resource[F, Write[F]] =
    for {
      incrementVideosWatchedCommand <- session.prepare(IncrementVideosWatched.command)
    } yield new Write[F] {
      override def incrementVideosWatched(globalPosition: Long): F[Unit] = 
        incrementVideosWatchedCommand.execute(globalPosition ~ globalPosition).void
    }

  def useEachTime[F[_]](r: Resource[F, Write[F]])(implicit F: MonadCancel[F, Throwable]): Write[F] = 
    new Write[F] {
      override def incrementVideosWatched(globalPosition: Long): F[Unit] = 
        r.use(_.incrementVideosWatched(globalPosition))
    }

  def useEachTime2[F[_]](r: Resource[F, Session[F]])(implicit F: MonadCancel[F, Throwable]): Write[F] = 
    useEachTime[F](r.flatMap(fromSession[F]))

}
