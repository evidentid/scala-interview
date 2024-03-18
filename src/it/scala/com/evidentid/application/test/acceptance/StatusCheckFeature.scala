package com.evidentid.application.test.acceptance

import com.evidentid.application.test.acceptance.setup.AcceptanceFeatureSpec

class StatusCheckFeature extends AcceptanceFeatureSpec {

  Feature("Status Check") {
    info("As a dev ops agent")
    info("I want to know the status of the service")
    info("So that I may verify its current state and health")

    Scenario("Check Service Status") {
      Given("The service is running")
      When("I request the status of the service")
      val eventualResponse = getServiceStatus()
      Then("I should be able to see its status details")
      whenReady(eventualResponse) { statusResponse =>
        statusResponse.buildHash shouldBe sys.env.getOrElse("BUILD_HASH", "unknown")
        statusResponse.buildNumber shouldBe sys.env.getOrElse("BUILD_NUMBER", "unknown")
        statusResponse.buildTag shouldBe sys.env.getOrElse("BUILD_TAG", "unknown")
        statusResponse.version shouldBe sys.env.getOrElse("APP_VERSION", "unknown")
        statusResponse.name shouldBe "eid-scala-app"
        statusResponse.status shouldBe "up"
      }
    }
  }
}
