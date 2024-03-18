package com.evidentid.http.server.formats

import akka.http.scaladsl.model.DateTime
import io.circe.{Decoder, Encoder, Json}
import sttp.tapir.{Schema, SchemaType}

trait DateTimeJsonFormat {

  implicit val encodeDateTime: Encoder[DateTime] = (a: DateTime) => Json.fromString(a.toIsoDateTimeString)

  implicit val decodeDateTime: Decoder[DateTime] = Decoder.decodeString.emap { str =>
    DateTime.fromIsoDateTimeString(str) match {
      case Some(dt) => Right(dt)
      case None     => Left(s"DateTime string must be in ISO 8601 format but was '$str'")
    }
  }

  implicit lazy val dateTimeSchema: Schema[DateTime] = Schema(schemaType = SchemaType.SString(), format = Some("date-time"))
}

object DateTimeJsonFormat extends DateTimeJsonFormat
