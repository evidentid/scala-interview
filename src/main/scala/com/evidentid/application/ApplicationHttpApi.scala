package com.evidentid.application

import com.evidentid.http.server.EndpointRoute
import com.evidentid.logging.Logging
import sttp.tapir.{endpoint, path, Endpoint}

trait ApplicationHttpApi extends EndpointRoute with Logging {

  def `/status`: Endpoint[Unit, Unit, Unit, Unit, Any] =
    endpoint
      .in("status")

  def `/rates`: Endpoint[Unit, Unit, Unit, Unit, Any] =
    endpoint
      .in("rates")

  def `/rates/<currency>` : Endpoint[Unit, String, Unit, Unit, Any] =
    `/rates`.in(
      path[String]
        .name("currencyCode")
        .description("Currency code.")
    )

}
