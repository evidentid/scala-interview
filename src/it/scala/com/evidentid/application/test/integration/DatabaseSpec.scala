package com.evidentid.application.test.integration

import com.evidentid.application.test.PostgresConnection
import com.evidentid.application.test.integration.setup.IntegrationSpec

class DatabaseSpec extends IntegrationSpec(requireSeparateDb = true) {

  "Database" should "be able to return status" in {
    whenReady(databaseManager.checkVersion()) { version =>
      version should include(PostgresConnection.productName)
      version should include(PostgresConnection.MAJOR_VERSION.toString)
    }
  }
}
