package com.evidentid.application

import com.evidentid.logging.Logging
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success}

object Main extends Logging {

  val DefaultApp = "DEFAULT_APP"

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val appMode = config.getString("app-mode")

    val app = appMode match {
      case DefaultApp => RunnableApplication(config)
      case _ =>
        throw new IllegalArgumentException(s"Invalid appMode found '$appMode'. Valid modes are: $DefaultApp.")
    }

    logger.info(s"Starting application...")
    val applicationWatchFuture = app.start()
    applicationWatchFuture.onComplete {
      case Failure(exception) => logger.error("Application failed to start, encountered error during app init.", exception)
      case Success(_)         =>
    }(app.system.dispatcher)
  }

}
