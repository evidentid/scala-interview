package com.evidentid.application

import akka.actor.{ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import com.evidentid.application.rates.RatesProviderManager
import com.evidentid.application.rates.api.RatesProviderRoute
import com.evidentid.application.status.HealthCheckManager
import com.evidentid.application.status.api.HealthCheckRoute
import com.evidentid.database.DatabaseManager
import com.evidentid.http.server.api.DocsRoute
import com.evidentid.logging.Logging
import com.typesafe.config.Config
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions

import java.net.InetSocketAddress
import java.time.{Clock, Instant}
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class RunnableApplication(
  val config: Config,
  val actorSystemName: String,
  val bindInterface: String,
  val bindPort: Int,
  val databaseConfig: DatabaseManager.DatabaseConnectionConfig
) extends Application
    with Logging {

  private var httpServerBinding: Future[InetSocketAddress] = _
  override lazy val system: ActorSystem = ActorSystem(actorSystemName, config)
  private lazy val ioExecutionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  override def start(): Future[Terminated] = {

    val appSetupStartTime = System.currentTimeMillis()
    logger.info("Starting Database setup")
    val databaseManager = setupDatabaseManager(databaseConfig)(system)
    val dbSetupDoneTime = System.currentTimeMillis()
    logger.info(s"Database setup done in ${dbSetupDoneTime - appSetupStartTime}ms, starting HTTP server")
    setupHttpServer(databaseManager)(system)
    val httpServerStartedTime = System.currentTimeMillis()
    logger.info(s"HTTP server started in ${httpServerStartedTime - dbSetupDoneTime}")
    val appSetupFinishedTime = System.currentTimeMillis()
    logger.info(s"Application started properly, total startup time: ${appSetupFinishedTime - appSetupStartTime}ms")

    system.whenTerminated
  }

  def setupDatabaseManager(databaseConfig: DatabaseManager.DatabaseConnectionConfig)(implicit system: ActorSystem): DatabaseManager = {
    logger.info("Creating database manager")
    val databaseManager = DatabaseManager.create(databaseConfig, disableFlywayHistoryClean = true)
    logger.info("Starting Flyway DB migration")
    databaseManager.migrate()
    logger.info("Database Flyway DB migration done, adding database shutdown task to coordinated shutdown")
    addDatabaseCoordinatedShutdownTask(databaseManager)
    logger.info("Database setup completed")

    databaseManager
  }

  def setupHttpServer(databaseManager: DatabaseManager)(implicit system: ActorSystem): Unit = {
    val http = Http()
    val routes = setupRoutes(databaseManager, system)
    httpServerBinding = startHttpServer(http, bindInterface, bindPort, routes)(system)
  }

  private def setupRoutes(databaseManager: DatabaseManager, system: ActorSystem): Route = {

    implicit val executionContext: ExecutionContextExecutor = ioExecutionContext
    implicit val actorSystem: ActorSystem = system.classicSystem

    implicit val serverSettings: AkkaHttpServerOptions = AkkaHttpServerOptions.default

    val healthCheckManager = HealthCheckManager(Instant.now, databaseManager)
    val healthCheckRoute = HealthCheckRoute(healthCheckManager)

    val ratesProviderManager = RatesProviderManager(databaseManager)
    val ratesProviderRoute = RatesProviderRoute(ratesProviderManager)

    val docsRoute = DocsRoute(healthCheckRoute.endpoints, ratesProviderRoute.endpoints)

    Route.seal(Directives.concat(docsRoute.route, healthCheckRoute.route, ratesProviderRoute.route))
  }

  def currentBinding: Future[InetSocketAddress] = httpServerBinding

}

object RunnableApplication {

  final val UtcClock = Clock.systemUTC()

  def apply(config: Config) = new RunnableApplication(
    config = config,
    actorSystemName = config.getString("application.actor-system-name"),
    bindInterface = config.getString("application.bind-interface"),
    bindPort = config.getInt("application.bind-port"),
    databaseConfig = DatabaseManager.DatabaseConnectionConfig(
      jdbcUrl = config.getString("database.jdbc-url"),
      numThreads = Some(config.getInt("database.connection-pool.number-of-threads")),
      queueSize = Some(config.getInt("database.connection-pool.queue-size")),
    ),
  )

}
