package com.evidentid.application.upstream

import akka.http.scaladsl.model.DateTime
import com.evidentid.http.client.HttpClient
import com.evidentid.http.server.formats.DateTimeJsonFormat
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

import java.util.UUID
import scala.annotation.nowarn
import scala.concurrent.Future
import scala.util.Random

@nowarn
class RateProvider(httpClient: HttpClient) {
  import com.evidentid.application.upstream.RateProvider._

  // currencyCode is needed only because of mocked data
  def getRate(url: String, currencyCode: String): Future[Seq[UpstreamRateResponse]] = {
    Future.successful(generateMockedData(currencyCode))
  }

  private def generateMockedData(currencyCode: String): Seq[UpstreamRateResponse] = {

    val currenciesWithExcludedInput = CurrenciesWithAvailableRates.filterNot(_ == currencyCode)

    Random
      .shuffle(currenciesWithExcludedInput)
      .take(Random.between(0, currenciesWithExcludedInput.size))
      .map { givenToCurrency =>
        UpstreamRateResponse(
          fromCurrency = currencyCode,
          toCurrency = givenToCurrency,
          rate = Random.between(0.5f, 100.2f),
          date = DateTime.now,
          providerId = UUID.randomUUID()
        )
      }
  }

}

object RateProvider extends DateTimeJsonFormat {
  final case class UpstreamRateResponse(fromCurrency: String, toCurrency: String, rate: Float, date: DateTime, providerId: UUID)

  object UpstreamRateResponse {
    implicit val codecForUpstreamRateResponse: Codec[UpstreamRateResponse] = deriveCodec[UpstreamRateResponse]
    implicit lazy val schemaForUpstreamRateResponse: Schema[UpstreamRateResponse] = Schema.derived[UpstreamRateResponse]
  }

  val CurrenciesWithAvailableRates = List("USD", "EUR", "PLN", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD")

  def apply(httpClient: HttpClient): RateProvider = new RateProvider(httpClient)
}
