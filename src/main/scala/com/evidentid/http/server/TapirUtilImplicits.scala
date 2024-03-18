package com.evidentid.http.server

import io.circe.generic.extras.Configuration
import io.circe.{KeyDecoder, KeyEncoder}
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.{Codec, DecodeResult, Schema, ValidationError, ValidationResult, Validator}

import scala.reflect.ClassTag

/** Trait for mixing in utility defs for Tapir constructs. Namely implicitly building schemas and validators from Circe codecs.
  */
trait TapirUtilImplicits {

  implicit def tapirSchemaForMap[K: KeyEncoder: KeyDecoder, V: Schema]: Schema[Map[K, V]] =
    Schema
      .schemaForMap(implicitly[Schema[V]])
      .map { map =>
        map
          .map {
            case (k, v) =>
              implicitly[KeyDecoder[K]].apply(k).map(_ -> v)
          }
          .collect {
            case Some(v) => v
          } match {
          case items if items.size == map.size => Some(items.toMap)
          case _                               => None
        }
      } {
        _.map {
          case (k, v) =>
            implicitly[KeyEncoder[K]].apply(k) -> v
        }
      }

  def plainCodecFromKeyEncoding[T](
    implicit encode: KeyEncoder[T],
    decode: KeyDecoder[T],
    config: Configuration,
    classTag: ClassTag[T]
  ): PlainCodec[T] =
    Codec.string
      .mapDecode { str =>
        decode(str).orElse(decode(config.transformConstructorNames(str))) match {
          case Some(value) => DecodeResult.Value(value)
          case None =>
            DecodeResult.InvalidValue(
              List(
                ValidationError(
                  Validator.Custom[T](_ => ValidationResult.Invalid()),
                  str,
                  customMessage = Some(s"$str is not a valid $classTag")
                )
              )
            )
        }
      } { t =>
        encode(t)
      }

}

object TapirUtilImplicits extends TapirUtilImplicits
