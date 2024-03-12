package com.evidentid.application.test.integration

import com.evidentid.application.test.integration.setup.IntegrationSpec
import com.evidentid.database.model.Tables

class RateCurrenciesCapabilitySpec extends IntegrationSpec(requireSeparateDb = true) {

  import com.evidentid.database.DatabaseProfile.api._

  "RateCurrencies" should "be retrievable from DB" in {

    val getTestRateProviders = Tables.RatesProviders.filter(_.currencyCode === "TEST").result

    // retrieved entity defined in V1__initial_schema.sql
    database.run(getTestRateProviders).futureValue.loneElement.providerName shouldBe "Test provider"
  }
}
