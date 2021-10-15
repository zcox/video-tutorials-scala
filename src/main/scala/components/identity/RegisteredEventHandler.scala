package vt.components.identity

import messagedb._
// import cats.syntax.all._
// import cats._
// import cats.effect._
// import java.util.UUID
import org.typelevel.log4cats.Logger

object RegisteredEventHandler {

  def handle[F[_]/*: Temporal*/: Logger](mdb: MessageDb[F], event: Registered): F[Unit] = {
    //shut up compiler
    val _ = (mdb, event)
    Logger[F].info("TODO implement RegisteredEventHandler.handle")
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
}
