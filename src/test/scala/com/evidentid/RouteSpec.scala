package com.evidentid

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions

import scala.concurrent.duration._

class RouteSpec extends UnitSpec with ScalatestRouteTest with FailFastCirceSupport {

  implicit def default(implicit system: ActorSystem): RouteTestTimeout = {
    val newDuration = 5.seconds
    system.log.info(s"Increasing route timeout from default 1 second to $newDuration to reduce risk of false positives on Jenkins")
    RouteTestTimeout(newDuration)
  }

  var routes: Route = _

  implicit val serverSettings: AkkaHttpServerOptions = AkkaHttpServerOptions.default

  override def createActorSystem(): ActorSystem =
    ActorTestKit().system.classicSystem

}
