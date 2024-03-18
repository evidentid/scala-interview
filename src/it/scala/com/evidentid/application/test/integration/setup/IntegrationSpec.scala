package com.evidentid.application.test.integration.setup

class IntegrationSpec(override val requireSeparateDb: Boolean = false) extends IntegrationSpecLike

object IntegrationSpec {

  final case class TestSetupException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

}
