package vt.components.email

import cats._
import cats.syntax.all._
import java.util.UUID
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import scala.util.control.NoStackTrace
import messagedb._

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
)

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
    implicit val decoder: Decoder[Data] = deriveDecoder[Data]
  }
  case class Metadata(
    originStreamName: String,
    traceId: UUID,
    userId: UUID,
  )
  object Metadata {
    implicit val decoder: Decoder[Metadata] = deriveDecoder[Metadata]
  }

  def from(d: Data, m: Metadata): Send = 
    Send(
      emailId = d.emailId,
      from = d.from,
      to = d.to,
      subject = d.subject,
      text = d.text,
      html = d.html,
      originStreamName = m.originStreamName,
      traceId = m.traceId,
      userId = m.userId,
    )
}

sealed trait Result

case class Sent(
  emailId: UUID,
  from: String,
  to: String,
  subject: String,
  text: String,
  html: String,
  originStreamName: String,
  traceId: UUID,
  userId: UUID,
) extends Result {
  def toMessage: MessageDb.Write.Message =
    MessageDb.Write.Message(
      id = UUID.randomUUID().toString,
      streamName = s"sendEmail-$emailId",
      `type` = "Sent",
      data = Sent.Data(
        emailId = emailId,
        from = from,
        to = to,
        subject = subject,
        text = text,
        html = html,
      ).asJson,
      metadata = Sent.Metadata(
        originStreamName = originStreamName,
        traceId = traceId,
        userId = userId,
      ).asJson.some,
      expectedVersion = none,
    )
}

object Sent {
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
    implicit val decoder: Decoder[Data] = deriveDecoder[Data]
  }
  case class Metadata(
    originStreamName: String,
    traceId: UUID,
    userId: UUID,
  )
  object Metadata {
    implicit val encoder: Encoder[Metadata] = deriveEncoder[Metadata]
    implicit val decoder: Decoder[Metadata] = deriveDecoder[Metadata]
  }

  def from(s: Send): Sent = 
    Sent(
      emailId = s.emailId,
      from = s.from,
      to = s.to,
      subject = s.subject,
      text = s.text,
      html = s.html,
      originStreamName = s.originStreamName,
      traceId = s.traceId,
      userId = s.userId,
    )

  def from(d: Data, m: Metadata): Sent = 
    Sent(
      emailId = d.emailId,
      from = d.from,
      to = d.to,
      subject = d.subject,
      text = d.text,
      html = d.html,
      originStreamName = m.originStreamName,
      traceId = m.traceId,
      userId = m.userId,
    )

  def from[F[_]](m: MessageDb.Read.Message)(implicit F: MonadError[F, Throwable]): F[Sent] =
    for {
      data <- m.decodeData[Sent.Data].liftTo[F]
      metadata <- m.decodeMetadata[Sent.Metadata].liftTo[F]
    } yield Sent.from(data, metadata)
}

case class Failed(
  emailId: UUID,
  from: String,
  to: String,
  subject: String,
  text: String,
  html: String,
  reason: String,
  originStreamName: String,
  traceId: UUID,
  userId: UUID,
) extends Result {
  def toMessage: MessageDb.Write.Message =
    MessageDb.Write.Message(
      id = UUID.randomUUID().toString,
      streamName = s"sendEmail-$emailId",
      `type` = "Failed",
      data = Failed.Data(
        emailId = emailId,
        from = from,
        to = to,
        subject = subject,
        text = text,
        html = html,
        reason = reason,
      ).asJson,
      metadata = Failed.Metadata(
        originStreamName = originStreamName,
        traceId = traceId,
        userId = userId,
      ).asJson.some,
      expectedVersion = none,
    )
}

object Failed {
  case class Data(
    emailId: UUID,
    from: String,
    to: String,
    subject: String,
    text: String,
    html: String,
    reason: String,
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

  def from(s: Send, reason: String): Failed = 
    Failed(
      emailId = s.emailId,
      from = s.from,
      to = s.to,
      subject = s.subject,
      text = s.text,
      html = s.html,
      reason = reason,
      originStreamName = s.originStreamName,
      traceId = s.traceId,
      userId = s.userId,
    )
}

case class Email(
  from: String,
  to: String,
  subject: String,
  text: String,
  html: String,
)
object Email {
  def from(s: Send): Email = 
    Email(
      from = s.from,
      to = s.to,
      subject = s.subject,
      text = s.text,
      html = s.html,
    )
}

case class AlreadySentError(sent: Sent) extends Exception with NoStackTrace with Result
case class EmailSendError(reason: String) extends Exception with NoStackTrace
