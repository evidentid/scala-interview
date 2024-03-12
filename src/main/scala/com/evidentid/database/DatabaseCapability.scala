package com.evidentid.database

import com.evidentid.application.status.HealthCheckCapability

import scala.concurrent.ExecutionContext

trait DatabaseCapability {

  final val queries = DatabaseCapability.Queries

  def database: DatabaseWrapper

  implicit def executionContext: ExecutionContext = database.ioExecutionContext
}

object DatabaseCapability {

  trait Queries extends HealthCheckCapability.Queries

  object Queries extends Queries
}
