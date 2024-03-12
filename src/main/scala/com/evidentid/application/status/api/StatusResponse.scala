package com.evidentid.application.status.api

import akka.http.scaladsl.model.DateTime
import com.evidentid.http.server.formats.DateTimeJsonFormat
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.json.circe.TapirJsonCirce

final case class StatusResponse(
  buildHash: String,
  buildNumber: String,
  buildTag: String,
  name: String,
  status: String,
  uptimeSeconds: Long,
  utcNow: DateTime,
  utcServiceStart: DateTime,
  version: String
)

object StatusResponse extends TapirJsonCirce with DateTimeJsonFormat {

  implicit val encodeStatusResponse: Encoder[StatusResponse] = deriveEncoder[StatusResponse]
  implicit val decodeStatusResponse: Decoder[StatusResponse] = deriveDecoder[StatusResponse]
  implicit lazy val statusResponseSchema: Schema[StatusResponse] = Schema.derived[StatusResponse]
}
