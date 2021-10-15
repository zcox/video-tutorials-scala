package vt.components.identity

import cats.syntax.all._
import messagedb._
import java.util.UUID
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import scala.util.control.NoStackTrace

case class Register(
  userId: UUID,
  email: String,
  passwordHash: String,
  traceId: UUID,
)

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

case class Registered(
  userId: UUID,
  email: String,
  passwordHash: String,
  traceId: UUID,
) {
  def toMessage: MessageDb.Write.Message = 
    MessageDb.Write.Message(
      id = UUID.randomUUID().toString,
      streamName = s"identity-$userId",
      `type` = "Registered",
      data = Registered.Data(userId, email, passwordHash).asJson,
      metadata = Registered.Metadata(traceId, userId).asJson.some,
      expectedVersion = -1L.some,
    )
}

object Registered {
  case class Data(
    userId: UUID,
    email: String,
    passwordHash: String,
  )
  object Data {
    implicit val encoder: Encoder[Data] = deriveEncoder[Data]
    implicit val decoder: Decoder[Data] = deriveDecoder[Data]
  }
  case class Metadata(
    userId: UUID,
    traceId: UUID,
  )
  object Metadata {
    implicit val encoder: Encoder[Metadata] = deriveEncoder[Metadata]
    implicit val decoder: Decoder[Metadata] = deriveDecoder[Metadata]
  }
}

case class UserLoginFailed(
  userId: UUID,
  reason: String,
  traceId: UUID,
)

object UserLoginFailed {
  case class Data(
    userId: UUID,
    reason: String,
  )
  object Data {
    implicit val decoder: Decoder[Data] = deriveDecoder[Data]
  }
  case class Metadata(
    traceId: UUID,
  )
  object Metadata {
    implicit val decoder: Decoder[Metadata] = deriveDecoder[Metadata]
  }
}

case class AccountLocked(
  userId: UUID,
  traceId: UUID,
) {
  def toMessage: MessageDb.Write.Message = 
    MessageDb.Write.Message(
      id = UUID.randomUUID().toString,
      streamName = s"identity-$userId",
      `type` = "AccountLocked",
      data = AccountLocked.Data(userId, "Too many login failures").asJson,
      metadata = AccountLocked.Metadata(traceId).asJson.some,
      expectedVersion = none,
    )
}

object AccountLocked {
  case class Data(
    userId: UUID,
    reason: String,
  )
  object Data {
    implicit val encoder: Encoder[Data] = deriveEncoder[Data]
  }
  case class Metadata(
    traceId: UUID,
  )
  object Metadata {
    implicit val encoder: Encoder[Metadata] = deriveEncoder[Metadata]
  }
}

case class Send(
  emailId: UUID,
  from: String,
  to: String,
  subject: String,
  text: String,
  html: String,
  originStreamName: String,
  traceId: UUID,
  userId: UUID,
) {
  def toMessage: MessageDb.Write.Message =
    MessageDb.Write.Message(
      id = UUID.randomUUID().toString,
      streamName = s"sendEmail:command-$emailId",
      `type` = "Send",
      data = Send.Data(
        emailId = emailId,
        from = from,
        to = to,
        subject = subject,
        text = text,
        html = html,
      ).asJson,
      metadata = Send.Metadata(
        originStreamName = originStreamName,
        traceId = traceId,
        userId = userId,
      ).asJson.some,
      expectedVersion = none,
    )
}

object Send {
  case class Data(
    emailId: UUID,
    from: String,
    to: String,
    subject: String,
    text: String,
    html: String,
  )
  object Data {
    implicit val encoder: Encoder[Data] = deriveEncoder[Data]
  }
  case class Metadata(
    originStreamName: String,
    traceId: UUID,
    userId: UUID,
  )
  object Metadata {
    implicit val encoder: Encoder[Metadata] = deriveEncoder[Metadata]
  }
}

case class AlreadyRegisteredError() extends Exception with NoStackTrace