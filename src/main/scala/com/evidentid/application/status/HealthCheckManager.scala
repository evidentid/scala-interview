package com.evidentid.application.status

import akka.http.scaladsl.model.DateTime
import com.evidentid.application.status.api.StatusResponse
import com.evidentid.database.DatabaseManager
import com.evidentid.logging.Logging

import java.lang.management.ManagementFactory
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class HealthCheckManager(utcServiceStart: DateTime, database: DatabaseManager)(implicit executionContext: ExecutionContext)
    extends Logging {
  import HealthCheckManager._
  val name = "eid-scala-app"

  def currentStatus(): Future[StatusResponse] = {
    val buildHash = sys.env.getOrElse("BUILD_HASH", "unknown")
    val buildNumber = sys.env.getOrElse("BUILD_NUMBER", "unknown")
    val buildTag = sys.env.getOrElse("BUILD_TAG", "unknown")
    val uptimeSeconds = ManagementFactory.getRuntimeMXBean.getUptime / 1000
    val utcNow = DateTime(Instant.now.toEpochMilli)
    val version = sys.env.getOrElse("APP_VERSION", "unknown")

    databaseStatus()
      .map { status =>
        StatusResponse(buildHash, buildNumber, buildTag, name, status, uptimeSeconds, utcNow, utcServiceStart, version)
      }
  }

  def databaseStatus(): Future[String] = {
    database
      .checkVersion()
      .map(_ => StatusUp)
      .recover { case NonFatal(_) => StatusDatabaseDown }
  }

}

object HealthCheckManager {

  val StatusUp = "up"
  val StatusDatabaseDown = "database down"

  def apply(utcServiceStart: DateTime, database: DatabaseManager)(implicit executionContext: ExecutionContext): HealthCheckManager = {
    new HealthCheckManager(utcServiceStart, database)
  }

  def apply(utcServiceStart: Instant, database: DatabaseManager)(implicit executionContext: ExecutionContext): HealthCheckManager = {
    apply(DateTime(utcServiceStart.toEpochMilli), database)
  }

}
