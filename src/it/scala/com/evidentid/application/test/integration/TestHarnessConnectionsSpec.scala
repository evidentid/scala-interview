package com.evidentid.application.test.integration

import com.evidentid.application.test.PostgresConnection
import com.evidentid.application.test.integration.setup.IntegrationSpec
import com.evidentid.database.DatabaseManager

class TestHarnessConnectionsSpec extends IntegrationSpec {

  "Docker Compose Hosted Postgres" should "be connectible" in {
    eventually {
      val connection = dataSource.getConnection
      try {
        connection.getMetaData.getDatabaseMajorVersion shouldBe PostgresConnection.MAJOR_VERSION
        connection.getMetaData.getDatabaseProductName shouldBe PostgresConnection.productName
      } finally {
        connection.close()
      }
    }
  }

  "Database" should "migrate from data source" in {
    val database =
      DatabaseManager.create(
        databaseConnectionConfig = DatabaseManager.DatabaseConnectionConfig(PostgresConnection.url(dbName = "evident")),
        migrationTarget = "latest",
        disableFlywayHistoryClean = false,
      )
    database.clear()
    val totalMigrations = database.totalMigrations
    val migratedCount = database.migrate()
    migratedCount should be(totalMigrations)
  }
}
