Scala implementation of the Video Tutorials example from the book [Practical Microservices: Build Event-Driven Architectures with Event Sourcing and CQRS](https://pragprog.com/titles/egmicro/practical-microservices/).

Uses pure FP libraries such as cats, cats-effect, fs2, http4s, circe, and skunk.

Run dependencies using:

```sh
docker-compose up --build -d`.
./run-basic-connector.sh
```

Run the application using `sbt reStart`.

Connect to messagedb using `PGPASSWORD=postgres psql -h localhost -p 5432 -U postgres -d message_store`.

Connect to the views postgres using `PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d postgres`.

Connect to Confluent Control Center at http://localhost:9021.

Record a viewing:

```sh
 curl -v -X POST -H 'X-User-Id: 5FAEC8C9-9A92-47C9-A838-250BD6C665E0' localhost:8080/videos/E86DE3C2-B8CD-449D-BBDC-58C194BD8FA0/viewings
```

Get the home page data:

```sh
curl -s -H 'X-User-Id: 5FAEC8C9-9A92-47C9-A838-250BD6C665E0' localhost:8080/home | jq
```

Register a new user:

```sh
curl -X POST -d '{"userId":"C7DDB894-A9EC-4E27-BFCA-5A8ABCDB2E83","email":"user@site.com","password":"asdf1234"}' localhost:8080/register
```
