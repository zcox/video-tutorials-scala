package vt.aggregators.usercredentials

import messagedb._
import cats._
import cats.syntax.all._
import cats.effect._
import fs2.Stream
import io.circe._
import io.circe.generic.semiauto._
import java.util.UUID

object UserCredentials {

  object Registered {
    case class Data(
      userId: UUID,
      email: String,
      passwordHash: String,
    )
    object Data {
      implicit val decoder: Decoder[Data] = deriveDecoder[Data]
    }
  }

  def aggregator[F[_]: Temporal](mdb: MessageDb[F], view: vt.views.usercredentials.Write[F]): Stream[F, Unit] = 
    mdb.subscribe(
      "identity",
      "aggregators:user-credentials",
      handle[F](view)
    )

  def handle[F[_]](view: vt.views.usercredentials.Write[F])(event: MessageDb.Read.Message)(implicit F: MonadError[F, Throwable]): F[Unit] = 
    event.`type` match {
      case "Registered" => 
        for {
          data <- event.decodeData[Registered.Data].liftTo[F]
          () <- view.createUserCredential(data.userId, data.email, data.passwordHash)
        } yield ()
      case _ => Applicative[F].unit
    }

}
