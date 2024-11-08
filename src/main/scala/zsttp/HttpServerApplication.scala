package zsttp

import zio._
import zio.logging.backend.SLF4J

object HttpServerApplication extends ZIOAppDefault {

  val HttpServerPort: Int = 8081

  override val bootstrap = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private def runApp() = {
    val program = for {
      httpServer <- ZIO.service[HttpApiServer]
      port = HttpServerPort
      serverFiver <- httpServer.run(port).fork
      _ <- ZIO.log(s"Server started at port $port")
      _ <- serverFiver.join
    } yield ()
    program
      .tapErrorCause(c => ZIO.logErrorCause("Application encountered an error", c))
      .provide(
        HttpApiServer.layer
      )
      .exitCode
  }

  override def run = {
    for {
      appFiber <- runApp().fork
      exitCode <- appFiber.join
      _ <- ZIO.logInfo("Application has stopped")
    } yield exitCode
  }
}
