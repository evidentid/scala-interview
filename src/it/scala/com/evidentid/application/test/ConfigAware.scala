package com.evidentid.application.test

import com.typesafe.config.{Config, ConfigFactory}

trait ConfigAware {
  val config: Config = ConfigFactory.load("application-integration-test")
}
