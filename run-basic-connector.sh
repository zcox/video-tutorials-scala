#!/bin/bash

# This is a very basic use of debezium: replicate all pg tables to kafka topics, no transforms, mostly default configs, etc.

curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d @- << EOF
{
  "name": "message-db-basic-source",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "plugin.name": "pgoutput",
    "database.hostname": "message-db",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname" : "message_store",
    "database.server.name": "message-db"
  }
}
EOF


# "transforms": "addPrefix",
# "transforms.addPrefix.type": "org.apache.kafka.connect.transforms.RegexRouter",
# "transforms.addPrefix.regex": ".*",
# "transforms.addPrefix.replacement": "debezium.basic.\$0"
