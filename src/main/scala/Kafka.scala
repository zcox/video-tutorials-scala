package vt

import messagedb._
import cats.Applicative
import cats.effect._
import fs2.Stream
import com.banno.kafka._
import com.banno.kafka.consumer._
import com.sksamuel.avro4s._
import scala.concurrent.duration._
import message_db.message_store.messages.{Key, Envelope, Value}
import java.time._

trait Kafka[F[_]] {
  def subscribe(
    category: String,
    subscriberId: String,
    f: MessageDb.Read.Message => F[Unit],
  ): Stream[F, Unit]
}

/*
{
  "before": null,
  "after": {
    "message_db.message_store.messages.Value": {
      "id": "93777286-dff7-40bc-85f4-7b5a49dde25c",
      "stream_name": "subscriberPosition-aggregators:home-page",
      "type": "Read",
      "position": 0,
      "global_position": 9,
      "data": {
        "string": "{\"position\": 8}"
      },
      "metadata": null,
      "time": 1632878460339628
    }
  },
  "source": {
    "version": "1.3.1.Final",
    "connector": "postgresql",
    "name": "message-db",
    "ts_ms": 1632878460363,
    "snapshot": {
      "string": "false"
    },
    "db": "message_store",
    "schema": "message_store",
    "table": "messages",
    "txId": {
      "long": 545
    },
    "lsn": {
      "long": 25178840
    },
    "xmin": null
  },
  "op": "c",
  "ts_ms": {
    "long": 1632878460846
  },
  "transaction": null
}
*/

object Kafka {

  implicit val keyFromRecord = FromRecord[Key]
  implicit val envelopeFromRecord = FromRecord[Envelope]

  def categoryName(streamName: String): String = 
    streamName.substring(0, streamName.indexOf('-'))
  def message(v: Value): MessageDb.Read.Message = 
    MessageDb.Read.Message(
      id = v.id,
      streamName = v.stream_name,
      `type` = v.`type`,
      position = v.position,
      globalPosition = v.global_position,
      data = v.data.getOrElse(throw new NullPointerException("TODO")),
      metadata = v.metadata,
      //TODO something about this time calculation is incorrect. apparently topic has microseconds
      time = LocalDateTime.ofInstant(Instant.ofEpochMilli(v.time / 1000L), ZoneOffset.UTC),
    )

  /** Subscribes to the messagedb CDC Kafka topic produced by Debezium, and filters on the specified category. */
  def fromDebeziumTopic[F[_]: Async](
    bootstrapServers: String,
    schemaRegistryUrl: String,
    topicName: String = "message-db.message_store.messages",
  ): Kafka[F] = 
    new Kafka[F] {
      def subscribe(
        category: String,
        subscriberId: String,
        f: MessageDb.Read.Message => F[Unit],
      ): Stream[F, Unit] = 
        Stream.resource(
          ConsumerApi.Avro4s.resource[F, Key, Envelope](
            BootstrapServers(bootstrapServers), 
            SchemaRegistryUrl(schemaRegistryUrl), 
            GroupId(s"consumer-$subscriberId"),
            EnableAutoCommit(false),
            AutoOffsetReset.earliest,
            ClientId(s"consumer-$subscriberId")
          )
        )
          .evalTap(_.subscribe(topicName))
          .flatMap(_.readProcessCommit(1.second) { c => 
            c.value.after.fold(Applicative[F].unit)(a => 
              if (categoryName(a.stream_name) == category)
                f(message(a))
              else
                Applicative[F].unit
            )
          })
    }
}
