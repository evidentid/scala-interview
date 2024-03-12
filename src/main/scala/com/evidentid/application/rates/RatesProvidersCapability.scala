package com.evidentid.application.rates

import com.evidentid.database.DatabaseCapability
import com.evidentid.database.model.Tables

import scala.concurrent.Future

trait RatesProvidersCapability extends DatabaseCapability {

  import com.evidentid.database.DatabaseProfile.api._

  def getFirstRateProvider(currencyCode: String): Future[Option[Tables.RatesProvider]] = {
    val dbAction = Tables.RatesProviders.filter(_.currencyCode === currencyCode).take(1).result.headOption

    database.run(dbAction)
  }

  def getRateProviders(currencyCode: String): Future[Seq[Tables.RatesProvider]] = {
    ???
  }

}
