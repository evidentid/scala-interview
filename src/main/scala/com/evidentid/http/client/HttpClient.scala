package com.evidentid.http.client

import akka.actor.ActorSystem
import com.evidentid.http.client.HttpClient.{HttpRequestException, HttpResponseException}
import io.circe
import io.circe.{Decoder, Encoder}
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.client3._
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3.circe._
import sttp.model.Uri.QuerySegment
import sttp.model.{Header, Method, StatusCode, Uri}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class HttpClient(backend: SttpBackend[Future, AkkaStreams with WebSockets])(implicit ec: ExecutionContext) {

  private def httpRequest[I: Encoder, O: Decoder](
    uri: Uri,
    method: Method,
    headers: Seq[Header],
    payload: Option[I],
    queries: Seq[QuerySegment]
  ): RequestT[Identity, Either[ResponseException[String, circe.Error], O], Any] = {

    val withoutRequestBody = RequestT[Identity, Either[String, String], Any](
      method = method,
      uri = uri.addQuerySegments(queries),
      body = NoBody,
      headers = headers,
      response = asString,
      options = RequestOptions(followRedirects = true, DefaultReadTimeout, maxRedirects = 32, redirectToGet = false),
      tags = Map()
    )

    payload
      .map(withoutRequestBody.body(_))
      .getOrElse(withoutRequestBody)
      .response(asJson[O])
  }

  private def performHttpRequest[O: Decoder](
    request: RequestT[Identity, Either[ResponseException[String, circe.Error], O], Any],
    expectedStatusCode: Option[StatusCode]
  ): Future[Response[O]] = {
    request
      .send(backend)
      .map {
        case response @ Response(Right(returnedData), _, _, _, _, _) if expectedStatusCode.forall(_ == response.code) =>
          response.copy(body = returnedData)
        case response @ Response(Left(error), _, _, _, _, _) =>
          throw HttpResponseException(
            s"Error during response parsing for '${response.request.method} ${response.request.uri}' HTTP request",
            response,
            Some(error)
          )
        case response =>
          throw HttpResponseException(
            s"Unexpected status code ${response.code} when expected $expectedStatusCode for '${response.request.method} ${response.request.uri}' HTTP request",
            response
          )
      }
      .recover {
        case NonFatal(ex) if !ex.isInstanceOf[HttpResponseException] =>
          throw HttpRequestException(s"Error during '${request.method} ${request.uri}' HTTP request execution", Some(ex))
      }
  }

  def get[O: Decoder](
    uri: Uri,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None
  ): Future[Response[O]] = {
    val request = httpRequest[None.type, O](uri = uri, method = Method.GET, headers = headers, payload = None, queries = queries)

    performHttpRequest(request, expectedStatusCode)
  }

  def post[I: Encoder, O: Decoder](
    uri: Uri,
    payload: I,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None
  ): Future[Response[O]] = {
    val request = httpRequest[I, O](uri = uri, method = Method.POST, headers = headers, payload = Some(payload), queries = queries)

    performHttpRequest(request, expectedStatusCode)
  }

  def put[I: Encoder, O: Decoder](
    uri: Uri,
    payload: I,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None
  ): Future[Response[O]] = {
    val request = httpRequest[I, O](uri = uri, method = Method.PUT, headers = headers, payload = Some(payload), queries = queries)

    performHttpRequest(request, expectedStatusCode)
  }

  def patch[I: Encoder, O: Decoder](
    uri: Uri,
    payload: I,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None
  ): Future[Response[O]] = {
    val request = httpRequest[I, O](uri = uri, method = Method.PATCH, headers = headers, payload = Some(payload), queries = queries)

    performHttpRequest(request, expectedStatusCode)
  }

  def delete[O: Decoder](
    uri: Uri,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None
  ): Future[Response[O]] = {
    val request = httpRequest[None.type, O](uri = uri, method = Method.DELETE, headers = headers, payload = None, queries = queries)

    performHttpRequest(request, expectedStatusCode)
  }

}

object HttpClient {

  def apply(actorSystem: ActorSystem)(implicit ec: ExecutionContext): HttpClient = new HttpClient(
    AkkaHttpBackend.usingActorSystem(actorSystem)
  )

  sealed class HttpClientException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

  final case class HttpRequestException private[HttpClient] (msg: String, cause: Option[Throwable] = None)
      extends HttpClientException(msg, cause)

  final case class HttpResponseException private[HttpClient] (msg: String, response: Response[_], cause: Option[Throwable] = None)
      extends HttpClientException(msg, cause)

}
