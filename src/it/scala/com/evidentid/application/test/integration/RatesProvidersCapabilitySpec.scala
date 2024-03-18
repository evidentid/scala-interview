package com.evidentid.application.test.integration

import com.evidentid.application.test.integration.setup.IntegrationSpec
import com.evidentid.database.model.Tables

class RatesProvidersCapabilitySpec extends IntegrationSpec(requireSeparateDb = true) {

  import com.evidentid.database.DatabaseProfile.api._

  "getFirstRateProvider" should "return None for non-existing currency" in {
    databaseManager.getFirstRateProvider("NONEXISTING").futureValue shouldBe None
  }

  it should "return rate provider for existing currency" in {
    // retrieved entity defined in V1__initial_schema.sql
    val currencyCode = "TEST"
    databaseManager.getFirstRateProvider(currencyCode).futureValue.value.currencyCode shouldBe currencyCode
  }

  "RatesProviders" should "example how use slick syntax to retrieve data in tests" in {

    val getTestRateProviders = Tables.RatesProviders.filter(_.currencyCode === "TEST").result

    // retrieved entity defined in V1__initial_schema.sql
    database.run(getTestRateProviders).futureValue.loneElement shouldBe databaseManager.getFirstRateProvider("TEST").futureValue.value
  }
}
