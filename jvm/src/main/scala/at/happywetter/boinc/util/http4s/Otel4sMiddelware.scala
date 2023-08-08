package at.happywetter.boinc.util.http4s

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.{Async, IO, Sync}
import org.http4s.{Header, HttpApp, Request}
import org.typelevel.ci.CIString
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{SpanKind, Status, Tracer}

object Otel4sMiddelware:

  implicit class Otel4sMiddelwareOps(service: HttpApp[IO])(implicit T: Tracer[IO]):
    def traced: HttpApp[IO] =
      Kleisli { (req: Request[IO]) =>
        T
          .spanBuilder("handle-incoming-request")
          .addAttribute(Attribute("http.method", req.method.name))
          .addAttribute(Attribute("http.url", req.uri.renderString))
          .addAttribute(
            Attribute("user_agent.original", req.headers.get(CIString("User-Agent")).map(_.head.value).getOrElse(""))
          )
          .addAttribute(Attribute("server.address", req.serverAddr.get.toString))
          .addAttribute(Attribute("server.port", req.serverPort.get.toString))
          .addAttribute(Attribute("client.address", req.remoteAddr.get.toString))
          .addAttribute(Attribute("url.path", req.uri.path.renderString))
          .addAttribute(Attribute("url.query", req.uri.query.renderString))
          .addAttribute(Attribute("url.scheme", req.uri.scheme.map(_.value).getOrElse("")))
          .withSpanKind(SpanKind.Server)
          .build
          .use { span =>
            for {
              reqBodySize <- req.body.compile.count
              _ <- span.addAttribute(Attribute("http.request.body.size", reqBodySize))

              response <- service(req).handleErrorWith { case error: Throwable =>
                IO { error.printStackTrace() } *>
                  span.recordException(error) *>
                  span.setStatus(Status.Error) *>
                  IO.raiseError(error)
              }

              // Can't do that here because that drains static file contents confusing Ember:
              _ <- OptionT.fromOption[IO](response.headers.get(CIString("Content-Length"))).foreachF {
                contentLengthHeader =>
                  span.addAttribute(Attribute("http.response.body.size", contentLengthHeader.head.value))
              }

              _ <- span.addAttribute(Attribute("http.status-code", response.status.code.toLong))
              _ <-
                if response.status.isSuccess then span.setStatus(Status.Ok)
                else span.setStatus(Status.Error)
            } yield
              val traceIdHeader = Header.Raw(CIString("traceId"), span.context.traceIdHex)
              response.putHeaders(traceIdHeader)
          }
      }
