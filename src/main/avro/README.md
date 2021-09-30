# Overview

The Avro schemas for the Debezium Kafka topic key and value were obtained from Schema Registry, and then used to generate Scala code.

## Avro Schema Files

These Avro schema files (avsc) were obtained from the Schema Registry:

```
curl -s localhost:8081/subjects/message-db.message_store.messages-key/versions/1 | jq
curl -s localhost:8081/subjects/message-db.message_store.messages-value/versions/1 | jq
```

The value of the `schema` field was unescaped (`\"` replaced by `"`) and saved to these files.

## Code Generation

[`sbt-avrohugger`](https://github.com/julianpeeters/sbt-avrohugger) was added to this project's build.sbt, and generates code from the avsc files. This code ends up in 
`target/scala-2.13/src_managed/main/compiled_avro/`.

These generated types can then be used with kafka4s:

```scala
ConsumerApi.Avro4s.resource[F, Key, Envelope]
```
