package vt.components.email

import cats._
import cats.syntax.all._
import cats.effect._
import java.util.UUID
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import fs2.Stream
import messagedb._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.util.control.NoStackTrace

object Email {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

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
  }

  case class Email(
    from: String,
    to: String,
    subject: String,
    text: String,
    html: String,
  )
  object Email {
    def from(s: Send.Data): Email = 
      Email(
        from = s.from,
        to = s.to,
        subject = s.subject,
        text = s.text,
        html = s.html,
      )
  }

  //TODO for fun, fail with EmailSendError some of the time
  def sendEmail[F[_]: Sync](email: Email): F[Unit] = 
    Logger[F].info(s"Sent email: $email")

  case object AlreadySentError extends Exception with NoStackTrace
  case class EmailSendError(reason: String) extends Exception with NoStackTrace

  def component[F[_]: Async](mdb: MessageDb[F]): Stream[F, Unit] = 
    mdb.subscribe(
      "sendEmail:command",
      "components:send-email",
      handle[F](mdb),
    )

  def handle[F[_]: Async](mdb: MessageDb[F])(command: MessageDb.Read.Message): F[Unit] = 
    command.`type` match {
      case "Send" => 
        command.decodeData[Send.Data].liftTo[F].flatMap(sendData => 
          command.decodeMetadata[Send.Metadata].liftTo[F].flatMap{sendMetadata => 
            val streamName = s"sendEmail-${sendData.emailId}"
            mdb
              .getAllStreamMessages(streamName)
              .find(_.`type` == "Sent")
              .compile
              .last
              .flatMap(maybeSent => 
                maybeSent.fold(
                  sendEmail(Email.from(sendData))
                    .flatMap(_ => 
                      mdb.writeMessage(
                        id = UUID.randomUUID().toString,
                        streamName = streamName,
                        `type` = "Sent",
                        data = Sent.Data(
                          emailId = sendData.emailId,
                          from = sendData.from,
                          to = sendData.to,
                          subject = sendData.subject,
                          text = sendData.text,
                          html = sendData.html,
                        ).asJson,
                        metadata = Sent.Metadata(
                          originStreamName = sendMetadata.originStreamName,
                          traceId = sendMetadata.traceId,
                          userId = sendMetadata.userId,
                        ).asJson.some,
                        expectedVersion = none,
                      ).void
                    )
                    .recoverWith {
                      case EmailSendError(r) => 
                        mdb.writeMessage(
                          id = UUID.randomUUID().toString,
                          streamName = streamName,
                          `type` = "Failed",
                          data = Failed.Data(
                            emailId = sendData.emailId,
                            from = sendData.from,
                            to = sendData.to,
                            subject = sendData.subject,
                            text = sendData.text,
                            html = sendData.html,
                            reason = r,
                          ).asJson,
                          metadata = Failed.Metadata(
                            originStreamName = sendMetadata.originStreamName,
                            traceId = sendMetadata.traceId,
                            userId = sendMetadata.userId,
                          ).asJson.some,
                          expectedVersion = none,
                        ).void
                    }
                )(event => 
                  Logger[F].warn(s"Ignoring command $command due to email already sent: $event")
                )
              )
          }
        )
      case _ => Applicative[F].unit
    }

}
