package vt.views.homepage

import cats.effect.MonadCancel
import skunk._
import skunk.implicits._
import skunk.codec.all._
import cats.effect.Resource

trait Read[F[_]] {
  def loadHomePage(): F[Long]
}

object Read {

  object LoadHomePage {
    val query: Query[Void, Long] = 
      sql"""
        SELECT (page_data->>'videosWatched')::bigint FROM pages WHERE page_name = 'home' LIMIT 1
      """.query(int8)
  }

  def fromSession[F[_]](session: Session[F]): Resource[F, Read[F]] =
    Resource.pure(new Read[F] {
      override def loadHomePage(): F[Long] = 
        session.unique(LoadHomePage.query)
    })

  def useEachTime[F[_]](r: Resource[F, Read[F]])(implicit F: MonadCancel[F, Throwable]): Read[F] = 
    new Read[F] {
      override def loadHomePage(): F[Long] = 
        r.use(_.loadHomePage())
    }

  def useEachTime2[F[_]](r: Resource[F, Session[F]])(implicit F: MonadCancel[F, Throwable]): Read[F] = 
    useEachTime[F](r.flatMap(fromSession[F]))

}
