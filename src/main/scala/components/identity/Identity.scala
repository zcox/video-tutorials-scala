package vt.components.identity

import messagedb._
import fs2.Stream
import cats.syntax.all._
import cats._
import cats.effect._
import java.util.UUID
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import scala.util.control.NoStackTrace
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Identity {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  object Register {
    case class Data(
      userId: UUID,
      email: String,
      passwordHash: String,
    )
    object Data {
      implicit val decoder: Decoder[Data] = deriveDecoder[Data]
    }
    case class Metadata(
      userId: UUID,
      traceId: UUID,
    )
    object Metadata {
      implicit val decoder: Decoder[Metadata] = deriveDecoder[Metadata]
    }
  }

  object Registered {
    case class Data(
      userId: UUID,
      email: String,
      passwordHash: String,
    )
    object Data {
      implicit val encoder: Encoder[Data] = deriveEncoder[Data]
    }
    case class Metadata(
      userId: UUID,
      traceId: UUID,
    )
    object Metadata {
      implicit val encoder: Encoder[Metadata] = deriveEncoder[Metadata]
    }
  }

  case object AlreadyRegisteredError extends Exception with NoStackTrace

  def component[F[_]: Async](mdb: MessageDb[F]): Stream[F, Unit] =
    mdb.subscribe(
      "identity:command",
      "components:identity:command",
      handle[F](mdb),
    )

  def handle[F[_]: Sync](mdb: MessageDb[F])(command: MessageDb.Read.Message): F[Unit] = 
    command.`type` match {
      case "Register" => 
        (for {
          registerData <- command.decodeData[Register.Data].liftTo[F]
          registerMetadata <- command.decodeMetadata[Register.Metadata].liftTo[F]
          identityId = registerData.userId
          streamName = s"identity-$identityId"
          //we only need to check if stream is empty; only need a single event to determine that
          events <- mdb.getStreamMessages(
            streamName = streamName,
            position = 0L.some,
            batchSize = 1L.some,
            condition = none,
          ).compile.toList
          () <- if (events.isEmpty) Applicative[F].unit else ApplicativeError[F, Throwable].raiseError[Unit](AlreadyRegisteredError)
          () <- mdb.writeMessage(
            UUID.randomUUID().toString,
            streamName,
            "Registered",
            Registered.Data(registerData.userId, registerData.email, registerData.passwordHash).asJson,
            Registered.Metadata(registerMetadata.traceId, registerMetadata.userId).asJson.some,
            -1L.some,
          ).void
        } yield ()).recoverWith {
          case AlreadyRegisteredError => Logger[F].warn(s"Identity already registered, ignoring command: $command")
        }
      case _ => Applicative[F].unit
    }

}
