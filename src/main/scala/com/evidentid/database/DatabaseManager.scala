package com.evidentid.database

import com.evidentid.application.rates.RatesProvidersCapability
import com.evidentid.application.status.HealthCheckCapability
import com.evidentid.logging.Logging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway

import scala.util.{Failure, Success, Try}

class DatabaseManager(val datasource: HikariDataSource, val database: DatabaseWrapper, val flyway: Flyway)
    extends HealthCheckCapability
    with RatesProvidersCapability
    with Logging {

  lazy val totalMigrations: Int = flyway.info().all().length

  def migrate(): Int = {
    val start = System.currentTimeMillis()
    logger.info("Starting flyway migration")
    val migrationExecutionTry = Try(flyway.migrate().migrationsExecuted)
    val delta = System.currentTimeMillis() - start
    migrationExecutionTry match {
      case Failure(exception) =>
        logger.error(s"Failed to apply migrations, error raised after ${delta}ms", exception)
        throw exception
      case Success(migrationsExecuted) =>
        logger.info(s"Applied $migrationsExecuted migrations in ${delta}ms.")
        migrationsExecuted
    }
  }

  def clear(): Unit = flyway.clean()
  def repair(): Unit = flyway.repair()

  def close(): Unit = {
    database.close()
    datasource.close()
  }

}

object DatabaseManager extends Logging {

  case class DatabaseConnectionConfig(jdbcUrl: String, numThreads: Option[Int] = None, queueSize: Option[Int] = None)

  Class.forName("org.postgresql.Driver")
  // import slick AsyncExecutor and Database types
  import slick.jdbc.PostgresProfile.api._

  def create(
    databaseConnectionConfig: DatabaseConnectionConfig,
    migrationTarget: String = "latest",
    disableFlywayHistoryClean: Boolean
  ): DatabaseManager = {
    val (hikariDatasource, slickDatabase) =
      prepareDatabase("eid-scala-app-db-async-executor", databaseConnectionConfig)
    val flywayMigrator = flyway(dataSource = hikariDatasource, target = migrationTarget, disableFlywayHistoryClean)

    new DatabaseManager(datasource = hikariDatasource, database = new DatabaseWrapper(slickDatabase), flyway = flywayMigrator)
  }

  private def prepareDatabase(executorName: String, config: DatabaseConnectionConfig): (HikariDataSource, Database) = {
    val numberOfThreads = config.numThreads.getOrElse(defaultNumThreads)
    val executorQueueSize = config.queueSize.getOrElse(defaultQueueSize(numberOfThreads))
    logger.info(
      s"Creating database connection $executorName: " +
        s"number of threads $numberOfThreads (provided via config ${config.numThreads}), " +
        s"executor queue size $executorQueueSize (provided via config ${config.queueSize})"
    )
    val datasource = createDataSource(poolName = s"$executorName-pool", jdbcUrl = config.jdbcUrl, numThreads = numberOfThreads)
    val asyncExecutor = createAsyncExecutor(executorName = executorName, numThreads = numberOfThreads, queueSize = executorQueueSize)
    (datasource, database(dataSource = datasource, maxConnections = numberOfThreads, asyncExecutor = asyncExecutor))
  }

  def createDataSource(poolName: String, jdbcUrl: String, numThreads: Int): HikariDataSource = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setPoolName(poolName)
    hikariConfig.setJdbcUrl(jdbcUrl)
    hikariConfig.setMaxLifetime(15 * 60 * 1000)
    hikariConfig.setMaximumPoolSize(numThreads)
    hikariConfig.setMinimumIdle(numThreads)
    hikariConfig.addDataSourceProperty("tcpKeepAlive", true)
    hikariConfig.setRegisterMbeans(true)
    new HikariDataSource(hikariConfig)
  }

  def database(dataSource: HikariDataSource, maxConnections: Int, asyncExecutor: AsyncExecutor): Database = {
    Database.forDataSource(dataSource, Some(maxConnections), asyncExecutor)
  }

  def flyway(
    dataSource: HikariDataSource,
    target: String,
    disableFlywayHistoryClean: Boolean,
    locations: List[String] = List("classpath:/com/evidentid/db/migrations"),
  ): Flyway = {
    Flyway
      .configure()
      .dataSource(dataSource)
      .cleanDisabled(disableFlywayHistoryClean)
      .locations(locations: _*)
      .target(target)
      .load()
  }

  private val numCores = Runtime.getRuntime.availableProcessors()
  private val defaultNumThreads = Math.max(2 * numCores, 10)
  private def defaultQueueSize(numThreads: Int): Int = numThreads * 100

  private def createAsyncExecutor(executorName: String, numThreads: Int, queueSize: Int): AsyncExecutor = {
    AsyncExecutor(
      name = executorName,
      minThreads = numThreads,
      maxThreads = numThreads,
      queueSize = queueSize,
      maxConnections = numThreads,
      registerMbeans = true
    )
  }

}
