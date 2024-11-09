package zsttp

import zio.http.netty.NettyConfig
import zio.http.{Body, Client, DnsResolver, Header, Headers, MediaType, Request, Response, URL, ZClient}
import zio.logging.backend.SLF4J
import zio.stream.{ZPipeline, ZStream}
import zio.{ZIOAppDefault, _}

import scala.util.Try

object ZioHttpClientMain extends ZIOAppDefault {
  override val bootstrap = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val countElementsUrl =
    URL.decode(s"http://localhost:${HttpServerApplication.HttpServerPort}/count-elements").toOption.get

  private def extractStringContentIfSucceeded(
      response: Response,
      actionDescription: String,
  ): Task[String] = {
    if (response.status.isSuccess) {
      response.body.asString
    } else {
      ZIO.fail(
        new Exception(s"Service responded with error while $actionDescription: ${response.status.code}")
      )
    }
  }

  def countStreamElementsRemotely(client: Client, ctns: ZStream[Any, Throwable, String]): Task[Int] = {
    val byteStream = ctns
      .via(ZPipeline.intersperse("\n") >>> ZPipeline.utf8Encode)
    val content = Body.fromStreamChunked(byteStream)
    val request = Request
      .post(countElementsUrl, content)
      .addHeaders(
        Headers(Header.ContentType(MediaType.application.json)) ++ Headers(Header.TransferEncoding.Chunked)
      )
    val program = for {
      response <- client.request(request)
      content <- extractStringContentIfSucceeded(response, "counting stream elements remotely")
      id <- ZIO.fromEither(Try.apply(content.toInt).toEither)
    } yield id
    ZIO.scoped(program)
  }

  override def run = {
    val program = for {
      _ <- ZIO.logInfo("Trying sttp streaming")
      client <- ZIO.service[Client]
      stream = ZStream.fromIterable(List("1", "2", "4", "8", "9"))
      count <- countStreamElementsRemotely(client, stream)
      _ <- ZIO.logInfo(s"Received: $count")
    } yield ()
    program
      .provide(
        zio.Runtime.removeDefaultLoggers,
        Client.live,
        ZLayer.succeed(ZClient.Config.default),
        DnsResolver.default,
        ZLayer.succeed(NettyConfig.default),
      )
      .tapErrorCause { c =>
        ZIO.logErrorCause("Exception occurred while running the try-out", c)
      }
      .exitCode
  }
}
