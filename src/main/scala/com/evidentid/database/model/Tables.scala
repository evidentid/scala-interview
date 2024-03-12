package com.evidentid.database.model

import akka.http.scaladsl.model.DateTime
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables {
  val profile = com.evidentid.database.DatabaseProfile

  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table RatesProviders
   *  @param id Database column id SqlType(uuid), PrimaryKey
   *  @param providerName Database column provider_name SqlType(text)
   *  @param currencyCode Database column currency_code SqlType(text)
   *  @param url Database column url SqlType(text)
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param modifiedAt Database column modified_at SqlType(timestamptz) */
  case class RatesProvider(id: java.util.UUID, providerName: String, currencyCode: String, url: String, createdAt: DateTime, modifiedAt: DateTime)
  /** GetResult implicit for fetching RatesProvider objects using plain SQL queries */
  implicit def GetResultRatesProvider(implicit e0: GR[java.util.UUID], e1: GR[String], e2: GR[DateTime]): GR[RatesProvider] = GR{
    prs => import prs._
      RatesProvider.tupled((<<[java.util.UUID], <<[String], <<[String], <<[String], <<[DateTime], <<[DateTime]))
  }
  /** Table description of table rates_providers. Objects of this class serve as prototypes for rows in queries. */
  class RatesProviders(_tableTag: Tag) extends profile.api.Table[RatesProvider](_tableTag, "rates_providers") {
    def * = (id, providerName, currencyCode, url, createdAt, modifiedAt).<>(RatesProvider.tupled, RatesProvider.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(providerName), Rep.Some(currencyCode), Rep.Some(url), Rep.Some(createdAt), Rep.Some(modifiedAt))).shaped.<>({r=>import r._; _1.map(_=> RatesProvider.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(uuid), PrimaryKey */
    val id: Rep[java.util.UUID] = column[java.util.UUID]("id", O.PrimaryKey)
    /** Database column provider_name SqlType(text) */
    val providerName: Rep[String] = column[String]("provider_name")
    /** Database column currency_code SqlType(text) */
    val currencyCode: Rep[String] = column[String]("currency_code")
    /** Database column url SqlType(text) */
    val url: Rep[String] = column[String]("url")
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[DateTime] = column[DateTime]("created_at")
    /** Database column modified_at SqlType(timestamptz) */
    val modifiedAt: Rep[DateTime] = column[DateTime]("modified_at")

    /** Uniqueness Index over (currencyCode,url) (database name rates_providers_currency_code_url_key) */
    val index1 = index("rates_providers_currency_code_url_key", (currencyCode, url), unique=true)
  }
  /** Collection-like TableQuery object for table RatesProviders */
  lazy val RatesProviders = new TableQuery(tag => new RatesProviders(tag))

  /** Entity class storing rows of table ArchivedRatesProviders
   *
   * @param id           Database column id SqlType(uuid), Default(None)
   * @param providerName Database column provider_name SqlType(text), Default(None)
   * @param currencyCode Database column currency_code SqlType(text), Default(None)
   * @param url          Database column url SqlType(text), Default(None)
   * @param createdAt    Database column created_at SqlType(timestamptz), Default(None)
   * @param modifiedAt   Database column modified_at SqlType(timestamptz), Default(None)
   * @param archivedAt   Database column archived_at SqlType(timestamptz) */
  case class ArchivedRatesProvider(id: Option[java.util.UUID] = None, providerName: Option[String] = None, currencyCode: Option[String] = None, url: Option[String] = None, createdAt: Option[DateTime] = None, modifiedAt: Option[DateTime] = None, archivedAt: DateTime)

  /** GetResult implicit for fetching ArchivedRatesProvider objects using plain SQL queries */
  implicit def GetResultArchivedRatesProvider(implicit e0: GR[Option[java.util.UUID]], e1: GR[Option[String]], e2: GR[Option[DateTime]], e3: GR[DateTime]): GR[ArchivedRatesProvider] = GR {
    prs =>
      import prs._
      ArchivedRatesProvider.tupled((<<?[java.util.UUID], <<?[String], <<?[String], <<?[String], <<?[DateTime], <<?[DateTime], <<[DateTime]))
  }

  /** Table description of table archived_rates_providers. Objects of this class serve as prototypes for rows in queries. */
  class ArchivedRatesProviders(_tableTag: Tag) extends profile.api.Table[ArchivedRatesProvider](_tableTag, "archived_rates_providers") {
    def * = (id, providerName, currencyCode, url, createdAt, modifiedAt, archivedAt).<>(ArchivedRatesProvider.tupled, ArchivedRatesProvider.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((id, providerName, currencyCode, url, createdAt, modifiedAt, Rep.Some(archivedAt))).shaped.<>({ r => import r._; _7.map(_ => ArchivedRatesProvider.tupled((_1, _2, _3, _4, _5, _6, _7.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(uuid), Default(None) */
    val id: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("id", O.Default(None))
    /** Database column provider_name SqlType(text), Default(None) */
    val providerName: Rep[Option[String]] = column[Option[String]]("provider_name", O.Default(None))
    /** Database column currency_code SqlType(text), Default(None) */
    val currencyCode: Rep[Option[String]] = column[Option[String]]("currency_code", O.Default(None))
    /** Database column url SqlType(text), Default(None) */
    val url: Rep[Option[String]] = column[Option[String]]("url", O.Default(None))
    /** Database column created_at SqlType(timestamptz), Default(None) */
    val createdAt: Rep[Option[DateTime]] = column[Option[DateTime]]("created_at", O.Default(None))
    /** Database column modified_at SqlType(timestamptz), Default(None) */
    val modifiedAt: Rep[Option[DateTime]] = column[Option[DateTime]]("modified_at", O.Default(None))
    /** Database column archived_at SqlType(timestamptz) */
    val archivedAt: Rep[DateTime] = column[DateTime]("archived_at")
  }

  /** Collection-like TableQuery object for table ArchivedRatesProviders */
  lazy val ArchivedRatesProviders = new TableQuery(tag => new ArchivedRatesProviders(tag))

}