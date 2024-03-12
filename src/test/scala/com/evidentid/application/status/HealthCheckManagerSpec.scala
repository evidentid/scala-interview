package com.evidentid.application.status

import akka.http.scaladsl.model.DateTime
import com.evidentid.UnitSpec
import com.evidentid.application.status.api.StatusResponse
import com.evidentid.database.DatabaseManager

import scala.concurrent.Future

class HealthCheckManagerSpec extends UnitSpec {

  var mockDatabaseManager: DatabaseManager = _
  var healthCheckManager: HealthCheckManager = _

  private val serviceStart = DateTime(year = 2020, month = 11, day = 8, hour = 10)

  before {
    mockDatabaseManager = mock[DatabaseManager]
    healthCheckManager = HealthCheckManager(serviceStart, mockDatabaseManager)
  }

  "databaseStatus" should "report 'up' on healthy database" in {
    (() => mockDatabaseManager.checkVersion())
      .expects()
      .once()
      .returning(Future.successful("version"))

    whenReady(healthCheckManager.databaseStatus()) { result =>
      result shouldBe "up"
    }
  }

  it should "report 'database down' on database query failure" in {
    (() => mockDatabaseManager.checkVersion())
      .expects()
      .once()
      .returning(Future.failed(new Exception("fake")))

    whenReady(healthCheckManager.databaseStatus()) { result =>
      result shouldBe "database down"
    }
  }

  "currentStatus" should "return sound timings" in {
    (() => mockDatabaseManager.checkVersion())
      .expects()
      .once()
      .returning(Future.successful("version"))

    whenReady(healthCheckManager.currentStatus()) { reportedStatus =>
      inside(reportedStatus) {
        case StatusResponse(buildHash, buildNumber, buildTag, name, status, uptimeSeconds, utcNow, utcServiceStart, version) =>
          buildHash shouldBe sys.env.getOrElse("BUILD_HASH", "unknown")
          buildNumber shouldBe sys.env.getOrElse("BUILD_NUMBER", "unknown")
          buildTag shouldBe sys.env.getOrElse("BUILD_TAG", "unknown")
          name shouldBe healthCheckManager.name
          status shouldBe "up"
          utcNow should be >= serviceStart
          utcNow should be <= DateTime.now
          uptimeSeconds should be >= 0L
          utcServiceStart shouldBe serviceStart
          version shouldBe sys.env.getOrElse("APP_VERSION", "unknown")
      }
    }
  }
}
