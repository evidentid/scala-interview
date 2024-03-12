package com.evidentid.application.test

import com.evidentid.http.client.HttpClient
import com.evidentid.http.client.HttpClient.HttpResponseException
import io.circe.{Decoder, Encoder}
import org.scalatest.EitherValues
import sttp.client3.DeserializationException
import sttp.model.Uri.QuerySegment
import sttp.model.{Header, StatusCode, Uri}

import scala.concurrent.{ExecutionContext, Future}

// TODO try use https://sttp.softwaremill.com/en/stable/ with akka-http backend to hide akka http streams complexity
class TestHttpClient(serverUri: Uri, httpClient: HttpClient)(implicit ex: ExecutionContext) extends EitherValues {

  private def prepareUri(baseUri: Option[Uri], serverUri: Uri, path: String): Uri = {
    val requestUri = baseUri.getOrElse(serverUri)
    requestUri.withPath(path.stripPrefix("/"))
  }

  def get[O: Decoder](
    path: String,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None,
    baseUri: Option[Uri] = None,
  ): Future[O] = {
    val uri = prepareUri(baseUri, serverUri, path)

    httpClient
      .get(uri, headers, queries, expectedStatusCode)
      .map(_.body)
  }

  def post[I: Encoder, O: Decoder](
    path: String,
    payload: I,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None,
    baseUri: Option[Uri] = None,
  ): Future[O] = {
    val uri = prepareUri(baseUri, serverUri, path)

    httpClient
      .post(uri, payload, headers, queries, expectedStatusCode)
      .map(_.body)
  }

  def put[I: Encoder, O: Decoder](
    path: String,
    payload: I,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None,
    baseUri: Option[Uri] = None,
  ): Future[O] = {
    val uri = prepareUri(baseUri, serverUri, path)

    httpClient
      .put(uri, payload, headers, queries, expectedStatusCode)
      .map(_.body)
  }

  def patch[I: Encoder, O: Decoder](
    path: String,
    payload: I,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None,
    baseUri: Option[Uri] = None,
  ): Future[O] = {
    val uri = prepareUri(baseUri, serverUri, path)

    httpClient
      .patch(uri, payload, headers, queries, expectedStatusCode)
      .map(_.body)
  }

  def delete[O: Decoder](
    path: String,
    headers: Seq[Header] = Seq.empty,
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None,
    baseUri: Option[Uri] = None,
  ): Future[O] = {
    val uri = prepareUri(baseUri, serverUri, path)

    httpClient
      .delete(uri, headers, queries, expectedStatusCode)
      .map(_.body)
  }

  // naive implementation but should be ok for test purposes,
  // for prod use cases we would need have separate client or dedicated API that don't trigger converting body to JSON
  def getWithRawResponseBody(
    path: String,
    headers: Seq[Header],
    queries: Seq[QuerySegment] = Seq.empty,
    expectedStatusCode: Option[StatusCode] = None,
    baseUri: Option[Uri] = None,
  ): Future[String] = {
    val uri = prepareUri(baseUri, serverUri, path)

    httpClient
      .get[String](uri, headers, queries, expectedStatusCode)
      .map(_.body)
      .recover {
        case ex: HttpResponseException =>
          ex.response.body
            .asInstanceOf[Either[DeserializationException[_], _]]
            .left
            .value
            .body
      }
  }

}

object TestHttpClient {

  def apply(serverUri: Uri, httpClient: HttpClient)(implicit ex: ExecutionContext): TestHttpClient =
    new TestHttpClient(serverUri, httpClient)

}
