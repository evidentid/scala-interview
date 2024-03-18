package com.evidentid.application.status

import com.evidentid.database.DatabaseCapability

import scala.concurrent.Future

trait HealthCheckCapability extends DatabaseCapability {

  import com.evidentid.application.status.HealthCheckCapability._

  def checkVersion(): Future[String] = {
    database.run(Queries.version)
  }

}

object HealthCheckCapability {

  trait Queries {
    import com.evidentid.database.DatabaseProfile.api._

    val version = sql"SELECT version()".as[String].head
  }

  object Queries extends Queries
}
