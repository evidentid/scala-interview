package com.evidentid.application.rates.api

import com.evidentid.application.ApplicationHttpApi
import com.evidentid.application.rates.RatesProviderManager
import com.evidentid.http.server.EndpointRoute.{ErrorInfo, RouteBinding}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions

import scala.concurrent.ExecutionContextExecutor

class RatesProviderRoute(manager: RatesProviderManager)(
  implicit val serverSettings: AkkaHttpServerOptions,
  val executionContextExecutor: ExecutionContextExecutor
) extends ApplicationHttpApi {

  val routeBindings: Seq[RouteBinding[_, _, _]] = Seq(RouteBinding(getRates) { currencyCode =>
    manager.getRates(currencyCode).endpointResult(s"Failed to get rates for $currencyCode")
  })

  private def getRates =
    `/rates/<currency>`.get
      .description("Get rates for given currency")
      .out(jsonBody[Seq[Rate]])
      .errorOut(jsonBody[ErrorInfo])

}

object RatesProviderRoute {

  def apply(
    manager: RatesProviderManager
  )(implicit serverSettings: AkkaHttpServerOptions, executionContextExecutor: ExecutionContextExecutor): RatesProviderRoute =
    new RatesProviderRoute(manager)

}
