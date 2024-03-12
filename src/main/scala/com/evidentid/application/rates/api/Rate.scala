package com.evidentid.application.rates.api

import akka.http.scaladsl.model.DateTime
import com.evidentid.http.server.formats.DateTimeJsonFormat
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema
import sttp.tapir.json.circe.TapirJsonCirce

import java.util.UUID

final case class Rate(fromCurrency: String, toCurrency: String, rate: Float, date: DateTime, rateProvider: UUID)

object Rate extends TapirJsonCirce with DateTimeJsonFormat {

  implicit val codecForRate: Codec[Rate] = deriveCodec[Rate]
  implicit lazy val schemaForRate: Schema[Rate] = Schema.derived[Rate]
}
