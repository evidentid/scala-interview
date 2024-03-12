package com.evidentid.application.status.api

import com.evidentid.application.ApplicationHttpApi
import com.evidentid.application.status.HealthCheckManager
import com.evidentid.http.server.EndpointRoute.{ErrorInfo, RouteBinding}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions

import scala.concurrent.ExecutionContextExecutor

class HealthCheckRoute(manager: HealthCheckManager)(
  implicit val serverSettings: AkkaHttpServerOptions,
  val executionContextExecutor: ExecutionContextExecutor
) extends ApplicationHttpApi {

  val routeBindings: Seq[RouteBinding[_, _, _]] = Seq(RouteBinding(getStatus) { _ =>
    manager.currentStatus().endpointResult("Failed to perform health check")
  })

  private def getStatus =
    `/status`.get
      .out(jsonBody[StatusResponse])
      .errorOut(jsonBody[ErrorInfo])

}

object HealthCheckRoute {

  def apply(
    manager: HealthCheckManager
  )(implicit serverSettings: AkkaHttpServerOptions, executionContextExecutor: ExecutionContextExecutor): HealthCheckRoute =
    new HealthCheckRoute(manager)

}
