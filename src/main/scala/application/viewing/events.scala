package vt.application.viewing

import cats.syntax.all._
import cats.Functor
import java.util.UUID
import messagedb.MessageDb
import io.circe.syntax._
import io.circe._
import io.circe.generic.semiauto._

object VideoViewed {

  case class Data(
      userId: UUID,
      videoId: UUID,
  )

  object Data {
    implicit val encoder: Encoder[Data] = deriveEncoder[Data]
  }

  case class Metadata(
      traceId: UUID,
      userId: UUID,
  )

  object Metadata {
    implicit val encoder: Encoder[Metadata] = deriveEncoder[Metadata]
  }
}

trait EventRepository[F[_]] {
  def recordViewing(userId: UUID, videoId: UUID, traceId: UUID): F[Unit]
}

object EventRepository {

  def categoryName = "viewing"
  //TODO this says videoId, but do we really want all viewed events for a videoId in the same stream? like millions of them?
  def streamName(videoId: UUID): String =
    s"$categoryName-${videoId.toString}"

  def apply[F[_]: Functor](messagedb: MessageDb[F]): EventRepository[F] =
    new EventRepository[F] {
      override def recordViewing(userId: UUID, videoId: UUID, traceId: UUID): F[Unit] = {
        val eventId = UUID.randomUUID()
        messagedb
          .writeMessage(
            eventId.toString,
            streamName(eventId),
            "VideoViewed",
            VideoViewed.Data(userId, videoId).asJson,
            VideoViewed.Metadata(traceId, userId).asJson.some,
            none,
          )
          .void
      }
    }
}
