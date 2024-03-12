package com.evidentid.application.status

import akka.http.scaladsl.model.{ContentTypes, DateTime, StatusCodes}
import akka.http.scaladsl.server.Route
import com.evidentid.RouteSpec
import com.evidentid.application.status.api.{HealthCheckRoute, StatusResponse}

import scala.concurrent.Future

class HealthCheckRouteSpec extends RouteSpec {

  private var mockManager: HealthCheckManager = _

  before {
    mockManager = mock[HealthCheckManager]
    val route = HealthCheckRoute(mockManager)
    routes = Route.seal(route.route)
  }

  "GET /status" should "Get the status of the service" in {
    val givenStatus = StatusResponse("a", "b", "c", "d", "e", 0L, DateTime.now, DateTime.now, "f")

    (() => mockManager.currentStatus())
      .expects()
      .once()
      .returning(Future.successful(givenStatus))

    val request = Get("/status")

    request ~> routes ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      entityAs[StatusResponse] shouldBe givenStatus
    }
  }
}
