package com.evidentid.database

sealed trait UpsertResult[+T] {
  def row: T
}

object UpsertResult {

  final case class Updated[T](old: T, row: T) extends UpsertResult[T]
  final case class Created[T](row: T)         extends UpsertResult[T]
}
