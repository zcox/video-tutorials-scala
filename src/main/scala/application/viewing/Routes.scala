package vt.application.viewing

import cats.syntax.all._
import cats.effect.{Resource, MonadCancel}
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import java.util.UUID
import vt.application.User

object Routes {

  def apply[F[_]](repoResource: Resource[F, EventRepository[F]])(implicit F: MonadCancel[F, Throwable]): AuthedRoutes[User, F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[User, F] {
      case POST -> Root / "videos" / UUIDVar(videoId) / "viewings" as user =>
        //I can't figure out how to actually use Natchez in a real system, so just create a random traceId for every request
        val traceId = UUID.randomUUID()
        repoResource.use { repo => 
          for {
            () <- repo.recordViewing(user.userId, videoId, traceId)
            response <- Ok(s"viewed $videoId")
          } yield response
        }
    }
  }
}
