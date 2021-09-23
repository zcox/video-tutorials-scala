package vt.application.homepage

import cats.syntax.all._
import cats.effect.MonadCancel
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import vt.application.User
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import org.http4s.circe._

object Routes {

  case class HomePageResponse(
    videosWatched: Long,
  )
  object HomePageResponse {
    implicit val encoder: Encoder[HomePageResponse] = deriveEncoder[HomePageResponse]
  }

  def apply[F[_]](view: vt.views.homepage.Read[F])(implicit F: MonadCancel[F, Throwable]): AuthedRoutes[User, F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[User, F] {
      case GET -> Root / "home" as _ =>
        for {
          count <- view.loadHomePage()
          response <- Ok(HomePageResponse(count).asJson)
        } yield response
    }
  }
}
