package zsttp

import zio._
import zio.http._
import zio.http.endpoint.Endpoint
import zio.stream.{ZPipeline, ZStream}

class HttpApiServer {

  private def handleCount(
      byteStream: ZStream[Any, Throwable, Byte]
  ): ZIO[Any, Throwable, Response] = {
    val stringStream = byteStream.via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
    stringStream.runCount.map { count =>
      Response(body = Body.fromString(count.toString))
    }
  }

  private val countElementsEndpoint = Endpoint(Method.POST / "count-elements")
  private val countElementsEndpointRoute: Route[Any, Nothing] = Route
    .route(countElementsEndpoint.route)
    .apply {
      Handler.fromFunctionZIO[Request] { request =>
        ZIO.logInfo("Received a request") *> {
          val stream = request.body.asStream
          val response = handleCount(stream)
          response.catchAll { th =>
            ZIO.logError(
              s"Exception occurred while handling client count segment request: ${th.getMessage}"
            ) *> ZIO.succeed(Response.internalServerError)
          }
        }
      }
    }

  private val app = Routes(countElementsEndpointRoute)

  def run(port: Int): ZIO[Any, Throwable, Nothing] = {
    Server
      .serve(app)
      .provide(
        Server
          .defaultWith { config =>
            config.port(port)
          }
      )
  }
}

object HttpApiServer {
  def layer: ULayer[HttpApiServer] = {
    ZLayer.succeed(new HttpApiServer)
  }
}
