package com.evidentid.application.test.acceptance.helpers

import akka.Done
import com.evidentid.application.test.TestHttpClient
import io.circe.{Json, JsonObject}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import sttp.model.{StatusCode, Uri}

import scala.concurrent.ExecutionContext

class MountebankProxy(httpClient: TestHttpClient)(implicit ec: ExecutionContext) extends ScalaFutures with OptionValues {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(250, Millis)))

  def mountebankRequests(mountebankUrl: String): Seq[Json] = {
    httpClient
      .get[JsonObject](path = "", baseUri = Some(Uri(mountebankUrl)), expectedStatusCode = Some(StatusCode.Ok))
      .futureValue
      .apply("requests")
      .value
      .asArray
      .value
  }

  def resetMountebankUri(mountebankUrl: String): Done = {
    httpClient
      .delete[None.type](path = "", baseUri = Some(Uri(mountebankUrl)), expectedStatusCode = Some(StatusCode.Ok))
      .map(_ => Done)
      .futureValue
  }

}

object MountebankProxy {
  def apply(httpClient: TestHttpClient)(implicit ec: ExecutionContext): MountebankProxy = new MountebankProxy(httpClient)
}
