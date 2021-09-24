package vt

import cats.syntax.all._
import cats.effect._
import org.http4s.implicits._
import messagedb._
import fs2.Stream
import org.http4s.server.AuthMiddleware
import vt.application.DumbAuth

import cats.effect.std.Console
import fs2.io.net.Network

object Main extends IOApp {

  def program[F[_]: Async: Network: Console]: Stream[F, ExitCode] = 
    Stream.resource(Database.messageDbSessionResource[F]).flatMap { messageDbSessionResource =>
      Stream.resource(Database.viewSessionResource[F]).flatMap { viewSessionResource => 

        val messageDbResource = messageDbSessionResource.flatMap(MessageDb.fromSession(_))
        val messageDb = MessageDb.useEachTime(messageDbResource)
        val eventRepositoryResource = messageDbResource.map(application.viewing.EventRepository(_))

        val identityComponent = vt.components.identity.Identity.component[F](messageDb)

        val migrate = Stream.eval(Flyway.migrate("localhost", 5433, "postgres", "postgres", "postgres")).drain
        val homePageViewWrite = views.homepage.Write.useEachTime2(viewSessionResource)
        val homePageViewRead = views.homepage.Read.useEachTime2(viewSessionResource)
        val userCredentialsViewRead = views.usercredentials.Read.useEachTime2(viewSessionResource)
        val userCredentialsViewWrite = views.usercredentials.Write.useEachTime2(viewSessionResource)

        val logViewings = aggregators.homepage.HomePage.logViewings[F](messageDb)
        val homePageAggregator = aggregators.homepage.HomePage.aggregator[F](messageDb, homePageViewWrite)
        val userCredentialsAggregator = aggregators.usercredentials.UserCredentials.aggregator[F](messageDb, userCredentialsViewWrite)

        val helloRoutes = application.hello.Routes[F]()
        val viewingRoutes = application.viewing.Routes[F](eventRepositoryResource)
        val homePageRoutes = application.homepage.Routes[F](homePageViewRead)
        val registerRoutes = application.register.Routes[F](messageDb, userCredentialsViewRead)
        val authMiddleware = AuthMiddleware(DumbAuth.impl[F])
        val routes = helloRoutes <+> registerRoutes <+> authMiddleware(viewingRoutes <+> homePageRoutes)
        val httpApp = routes.orNotFound

        migrate ++ Stream(
          HttpServer.stream[F](httpApp),
          logViewings,
          homePageAggregator,
          userCredentialsAggregator,
          identityComponent,
        ).parJoinUnbounded.as(ExitCode.Success)
      }
    }

  override def run(args: List[String]): IO[ExitCode] = 
    program[IO].compile.drain.as(ExitCode.Success)

}
