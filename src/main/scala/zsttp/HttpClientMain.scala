package zsttp

import sttp.capabilities.zio.ZioStreams
import sttp.client3
import sttp.client3.httpclient.zio.SttpClient
import sttp.client3.{Response, UriContext, asStreamAlwaysUnsafe}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.{Header, Method, Uri}
import zio.{ZIOAppDefault, _}
import zio.logging.backend.SLF4J
import zio.stream.{ZPipeline, ZStream}

import scala.util.Try

object HttpClientMain extends ZIOAppDefault {
  override val bootstrap = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val countElementsUrl =
    Uri.unsafeParse(s"http://localhost:${HttpServerApplication.HttpServerPort}/count-elements")

  private def extractStringContentIfSucceeded(
      response: Response[String],
      actionDescription: String,
  ): Task[String] = {
    if (response.isSuccess) {
      ZIO.succeed(response.body)
    } else {
      ZIO.fail(
        new Exception(s"Service responded with error while $actionDescription: ${response.code.code}")
      )
    }
  }

  def countStreamElementsRemotely(client: SttpClient, ctns: ZStream[Any, Throwable, String]): Task[Int] = {
    val byteStream = ctns
      .via(ZPipeline.intersperse("\n") >>> ZPipeline.utf8Encode)
    val request = client3.basicRequest
      .post(countElementsUrl)
      .streamBody(ZioStreams)(byteStream)
      .response(client3.asStringAlways)
    val program = for {
      response <- client.send(request)
      content <- extractStringContentIfSucceeded(response, "counting stream elements remotely")
      id <- ZIO.fromEither(Try.apply(content.toInt).toEither)
    } yield id
    ZIO.scoped(program)
  }

  override def run = {
    val program = for {
      _ <- ZIO.logInfo("Trying sttp streaming")
      client <- ZIO.service[SttpClient]
      stream = ZStream.fromIterable(List("1", "2", "4", "8", "9"))
      count <- countStreamElementsRemotely(client, stream)
      _ <- ZIO.logInfo(s"Received: $count")
    } yield ()
    program
      .provide(
        zio.Runtime.removeDefaultLoggers,
        HttpClientZioBackend.layer(),
      )
      .tapErrorCause { c =>
        ZIO.logErrorCause("Exception occurred while running the try-out", c)
      }
      .exitCode
  }
}
