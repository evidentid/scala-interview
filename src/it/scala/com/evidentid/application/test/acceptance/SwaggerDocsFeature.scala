package com.evidentid.application.test.acceptance

import com.evidentid.application.test.acceptance.setup.AcceptanceFeatureSpec

class SwaggerDocsFeature extends AcceptanceFeatureSpec {

  Feature("Swagger Docs") {
    info("As an engineer")
    info("I want to know the view openapi documentation")
    info("So that I may know how to consumer the API")

    Scenario("Get swagger documentation") {
      Given("The service is up")
      When("I GET service docs")
      val response = getDocs()
      Then("A yaml openapi doc is returned")
      whenReady(response) { docs =>
        // you can use this AT to simulate the app running locally, just uncomment below lines
//        println(appBinding)
//        Thread.sleep(1000000)
        docs should startWith("openapi: 3.0.3")
        docs should include("title: EID Scala app")
        docs should include("paths:")
        docs should include("components:")
        succeed
      }
    }
  }
}
