package com.evidentid.http.server.api

import com.evidentid.http.server.EndpointRoute
import com.evidentid.http.server.EndpointRoute.RouteBinding
import sttp.apispec.openapi.circe.yaml._
import sttp.tapir.docs.openapi._
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions
import sttp.tapir.{endpoint, stringBody, AnyEndpoint}

import scala.concurrent.{ExecutionContextExecutor, Future}

class DocsRoute(endpointsToIncludeInDocs: Seq[AnyEndpoint])(
  implicit val serverSettings: AkkaHttpServerOptions,
  val executionContextExecutor: ExecutionContextExecutor
) extends EndpointRoute {

  private lazy val docs: String = {
    val version = sys.env.getOrElse("APP_VERSION", "unknown")
    val eps = endpointsToIncludeInDocs ++ this.endpoints
    val docs = OpenAPIDocsInterpreter(OpenAPIDocsOptions.default).toOpenAPI(eps, "EID Scala app", version)
    docs.toYaml
  }

  val routeBindings: Seq[RouteBinding[_, _, _]] = Seq(RouteBinding(getDocs) { _ =>
    Future.successful(Right(docs))
  })

  private def getDocs =
    endpoint
      .in("docs")
      .out(stringBody)
      .get

}

object DocsRoute {

  def apply(
    endpointsToIncludeInDocs: Seq[AnyEndpoint]*
  )(implicit serverSettings: AkkaHttpServerOptions, executionContextExecutor: ExecutionContextExecutor): DocsRoute = {
    new DocsRoute(endpointsToIncludeInDocs.flatten)
  }

}
