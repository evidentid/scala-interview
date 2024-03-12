package com.evidentid.http.server

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{logRequestResult, withExecutionContext}
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.{Directives, Route, RouteResult}
import com.evidentid.logging.Logging
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.{Endpoint, Schema}
import sttp.tapir.server.akkahttp.{AkkaHttpServerInterpreter, AkkaHttpServerOptions}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

trait EndpointRoute extends TapirUtilImplicits with Logging {
  import EndpointRoute._

  implicit def serverSettings: AkkaHttpServerOptions

  def routeBindings: Seq[RouteBinding[_, _, _]]

  def executionContextExecutor: ExecutionContextExecutor

  def route: Route = logRequestResult(requestBasicInfoAndResponseStatus _) {
    Directives.concat(routeBindings.map(_.route(serverSettings, executionContextExecutor)): _*)
  }

  def endpoints: Seq[Endpoint[_, _, _, _, AkkaStreams with WebSockets]] =
    routeBindings.map(_.endpoint)

  private def requestBasicInfoAndResponseStatus(req: HttpRequest): RouteResult => Option[LogEntry] = {
    case RouteResult.Complete(res) if res.status.isFailure() =>
      Some(LogEntry(s"Request ${req.method} to ${req.uri} resulted in failure with status ${res.status}", akka.event.Logging.InfoLevel))
    case RouteResult.Complete(res) =>
      Some(LogEntry(s"Request ${req.method} to ${req.uri} resulted in response with status ${res.status}", akka.event.Logging.DebugLevel))
    case RouteResult.Rejected(rejections) if rejections.nonEmpty =>
      Some(LogEntry(s"Request ${req.method} to ${req.uri} was rejected with rejections: $rejections", akka.event.Logging.InfoLevel))
    case _ => None
  }

  implicit class EndpointResultFutureOps[T](future: Future[T]) {

    def endpointResult(successMessage: T => String, errorMessage: => String)(implicit ec: ExecutionContext): EndpointResult[T] =
      future
        .map { result =>
          logger.info(successMessage(result))
          Right(result)
        }
        .recover {
          case NonFatal(ex) =>
            logger.error(errorMessage, ex)
            Left(ErrorInfo.apply(ex))
        }

    def endpointResult(errorMessage: => String)(implicit ec: ExecutionContext): EndpointResult[T] =
      future
        .map(Right(_))
        .recover {
          case NonFatal(ex) =>
            logger.error(errorMessage, ex)
            Left(ErrorInfo.apply(ex))
        }

    def endpointResult()(implicit ec: ExecutionContext): EndpointResult[T] =
      future
        .map(Right(_))
        .recover {
          case NonFatal(ex) =>
            Left(ErrorInfo.apply(ex))
        }

  }

}

object EndpointRoute {

  final case class ErrorInfo(message: String)

  object ErrorInfo {

    def apply(ex: Throwable): ErrorInfo = ErrorInfo(ex.getMessage)

    implicit val codecForErrorInfo: Codec[ErrorInfo] = deriveCodec[ErrorInfo]
    implicit lazy val schemaForErrorInfo: Schema[ErrorInfo] = Schema.derived[ErrorInfo]
  }

  type EndpointResult[T] = Future[Either[ErrorInfo, T]]

  final case class RouteBinding[I, E, O](endpoint: Endpoint[Unit, I, E, O, AkkaStreams with WebSockets])(logic: I => Future[Either[E, O]]) {

    def route(implicit serverSettings: AkkaHttpServerOptions, executionContextExecutor: ExecutionContextExecutor): Route = {
      withExecutionContext(executionContextExecutor) {
        AkkaHttpServerInterpreter(serverSettings).toRoute(endpoint.serverLogic(logic))
      }
    }

  }

}
