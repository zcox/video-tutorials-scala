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

  case object AlreadyRegisteredError extends Exception with NoStackTrace

  def component[F[_]: Async](mdb: MessageDb[F]): Stream[F, Unit] = {
    val identityCommands = mdb.subscribe(
      "identity:command",
      "components:identity:command",
      handleIdentityCommand[F](mdb),
    )
    // val identityEvents = mdb.subscribe(
    //   "identity",
    //   "components:identity",
    //   e => e.`type` match {
    //     case "Registered" => handleRegistered[F](mdb, e)
    //     case _ => Applicative[F].unit
    //   }
    // )
    //lock account on too many login failures. this could be a separate component, if multiple components can write events to same category stream (i.e. identity)
    val authenticationEvents = mdb.subscribe(
      "authentication",
      "components:identity:authentication",
      handleAuthenticationEvent[F](mdb),
    )
    identityCommands/*.merge(identityEvents)*/.merge(authenticationEvents)
  }

  def handleIdentityCommand[F[_]: Sync](mdb: MessageDb[F])(command: MessageDb.Read.Message): F[Unit] = 
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

  // def handleRegistered[F[_]](mdb: MessageDb[F], event: MessageDb.Read.Message): F[Unit] = 
  //   for {
  //     registeredData <- event.decodeData[Registered.Data].liftTo[F]
  //     registeredMetadata <- event.decodeMetadata[Registered.Metadata].liftTo[F]
  //     maybeSent <- mdb
  //       .getAllStreamMessages(s"identity-${registeredData.userId}")
  //       .find(_.`type` == "RegistrationEmailSent")
  //       .compile
  //       .last
  //     () <- maybeSent.fold{
  //       val emailId = UUID.nameUUIDFromBytes(identity.email.getBytes("UTF-8"))
  //       mdb.writeMessage(
  //         id = UUID.randomUUID().toString,
  //         streamName = s"sendEmail:command-$emailId",
  //         `type` = "Send",
  //         data = Send.Data(
  //           emailId = emailId,
  //           from = "todo@site.com",
  //           to = identity.email,
  //           subject = "Welcome",
  //           text = "You registered!",
  //           html = "<p>You registered!</p>",
  //         ).asJson,
  //         metadata = Send.Metadata(
  //           originStreamName = sendMetadata.originStreamName,
  //           traceId = sendMetadata.traceId,
  //           userId = sendMetadata.userId,
  //         ).asJson.some,
  //         expectedVersion = none,
  //       ).void
  //     }(_ => Applicative[F].unit)
  //   } yield ()

  def handleAuthenticationEvent[F[_]: Sync](mdb: MessageDb[F])(event: MessageDb.Read.Message): F[Unit] = 
    event.`type` match {
      case "UserLoginFailed" => 
        for {
          userLoginFailedData <- event.decodeData[UserLoginFailed.Data].liftTo[F]
          userLoginFailedMetadata <- event.decodeMetadata[UserLoginFailed.Metadata].liftTo[F]
          // if userLoginFailedData.reason == "Incorrect password"
          userId = userLoginFailedData.userId
          streamName = s"authentication-$userId"
          //TODO this would need to be more sophisticated in real life, this is just proof-of-concept
          () <- mdb
            .getStreamMessages(streamName, 0L.some, Long.MaxValue.some, none)
            .collect { case e if e.`type` == "UserLoginFailed" /*and reason is "Incorrect password" and within 1h and after unlocked....*/ => 1 }
            .compile
            .fold(0)(_ + _)
            .map(_ > 3)
            .ifM(writeAccountLockedEvent(mdb, userId, userLoginFailedMetadata.traceId), Applicative[F].unit)
        } yield ()
      case _ => Applicative[F].unit
    }

  def writeAccountLockedEvent[F[_]: Functor](mdb: MessageDb[F], userId: UUID, traceId: UUID): F[Unit] =
    mdb.writeMessage(
      UUID.randomUUID().toString,
      s"identity-$userId",
      "AccountLocked",
      AccountLocked.Data(userId, "Too many login failures").asJson,
      AccountLocked.Metadata(traceId).asJson.some,
      none,
    ).void

}
