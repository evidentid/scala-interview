package com.evidentid.application.rates

import akka.http.scaladsl.model.DateTime
import com.evidentid.application.rates.api.Rate
import com.evidentid.database.DatabaseManager
import com.evidentid.logging.Logging

import java.util.UUID
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

@nowarn
class RatesProviderManager(database: DatabaseManager)(implicit executionContext: ExecutionContext) extends Logging {

  def getRates(currencyCode: String): Future[Seq[Rate]] = {
    // TODO provide real implementation
    val exampleRate =
      Rate(fromCurrency = currencyCode, toCurrency = "EUR", rate = 1.2f, date = DateTime.now, rateProvider = UUID.randomUUID())
    Future.successful(Seq(exampleRate))
  }

}

object RatesProviderManager {

  def apply(database: DatabaseManager)(implicit executionContext: ExecutionContext): RatesProviderManager = {
    new RatesProviderManager(database)
  }

}
