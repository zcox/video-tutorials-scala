Global / onChangedBuildSource := ReloadOnSourceChanges

scalaVersion := "2.13.6"

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full)

lazy val V = new {
  val flyway = "8.0.0-beta2"
  val http4s = "0.23.1"
  val kafka4s = "4.0.0-M5"
  val messagedb4s = "0.1.0-SNAPSHOT"
  val munit = "0.7.27"
  val munitCatsEffect3 = "1.0.0"
  val postgres = "42.2.14"
  val skunk = "0.2.2"
}

libraryDependencies ++= Seq(
  "org.tpolecat" %% "skunk-circe" % V.skunk,
  "org.flywaydb" % "flyway-core" % V.flyway,
  "org.postgresql" % "postgresql" % V.postgres,
  "default" %% "messagedb4s" % V.messagedb4s,
  "org.http4s" %% "http4s-dsl" % V.http4s,
  "org.http4s" %% "http4s-blaze-server" % V.http4s,
  "org.http4s" %% "http4s-circe" % V.http4s,
  "io.circe" %% "circe-generic" % "0.14.1",
  // "org.tpolecat" %% "natchez-log" % "0.1.5",
  // "org.tpolecat" %% "natchez-http4s" % "0.1.3",
  "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
  "ch.qos.logback" % "logback-classic" % "1.2.5",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0",
  "com.banno" %% "kafka4s" % V.kafka4s,
  "org.scalameta" %% "munit" % V.munit % Test,
  "org.typelevel" %% "munit-cats-effect-3" % V.munitCatsEffect3 % Test,
)

Compile / sourceGenerators += (Compile / avroScalaGenerate).taskValue,
