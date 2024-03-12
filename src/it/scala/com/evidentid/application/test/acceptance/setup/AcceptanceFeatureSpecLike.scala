package com.evidentid.application.test.acceptance.setup

import akka.actor.Terminated
import akka.actor.testkit.typed.scaladsl.LogCapturing
import com.evidentid.application.RunnableApplication
import com.evidentid.application.status.api.StatusResponse
import com.evidentid.application.test.acceptance.helpers.MountebankProxy
import com.evidentid.application.test.{ConfigAware, PostgresConnection, TestHttpClient}
import com.evidentid.database.DatabaseManager
import com.evidentid.http.client.HttpClient
import com.typesafe.config.{Config, ConfigValueFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import sttp.model.Uri

import java.net.InetSocketAddress
import scala.concurrent.{Await, ExecutionContext, Future}

/** Base trait for acceptance test.
  *
  * Please extend @link AcceptanceFeatureSpec} if possible, as it might improve compilation time compared to mixing-in a trait.
  */
trait AcceptanceFeatureSpecLike
    extends AnyFeatureSpec
    with BeforeAndAfter
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Eventually
    with GivenWhenThen
    with Matchers
    with ScalaFutures
    with LogCapturing
    with OptionValues
    with EitherValues
    with Inside
    with LoneElement
    with ConfigAware {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(250, Millis)))

  val dbName = "eid_application_acceptance_tests"
  val migrationTarget = "latest"

  var dbManager: DatabaseManager = _

  var app: RunnableApplication = _
  var appBinding: InetSocketAddress = _
  var appShutdown: Future[Terminated] = _
  var httpClient: TestHttpClient = _
  var mountebankProxy: MountebankProxy = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    AcceptanceFeatureSpec.NumberOfSpecsInProgress.incrementAndGet()
    waitForAppContextCreation()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    if (AcceptanceFeatureSpec.NumberOfSpecsInProgress.decrementAndGet() == 0) {
      dbManager.clear()
      AcceptanceFeatureSpec.App.get().terminate()
      Await.ready(AcceptanceFeatureSpec.AppShutdown.get(), patienceConfig.timeout)
    }
    dbManager.close()
  }

  private def waitForAppContextCreation(): Unit = {
    if (AcceptanceFeatureSpec.IsAppInitStarted.compareAndSet(false, true)) {
      createPostgresDb(dbName)
      dbManager = createDbManager(dbName)
      executeFlywayMigration(dbManager)
      app = spawnApp()
      AcceptanceFeatureSpec.App.set(app)
      appShutdown = app.start()
      AcceptanceFeatureSpec.AppShutdown.set(appShutdown)
      eventually {
        appBinding = Await.result(app.currentBinding, patienceConfig.timeout)
      }
      AcceptanceFeatureSpec.AppBinding.set(appBinding)
      httpClient = TestHttpClient(currentUri, HttpClient.apply(app.system))
      mountebankProxy = MountebankProxy(httpClient)
      resetMountebankRequests()
      AcceptanceFeatureSpec.IsAppAlreadyInit.set(true)
    } else {
      eventually(timeout(scaled(Span(60, Seconds))), interval(scaled(Span(200, Millis)))) {
        if (AcceptanceFeatureSpec.IsAppAlreadyInit.get() && AcceptanceFeatureSpec.AppBinding.get() != null) {
          app = AcceptanceFeatureSpec.App.get()
          appShutdown = AcceptanceFeatureSpec.AppShutdown.get()
          appBinding = AcceptanceFeatureSpec.AppBinding.get()
          dbManager = createDbManager(dbName)
          httpClient = TestHttpClient(currentUri, HttpClient.apply(app.system))
          mountebankProxy = MountebankProxy(httpClient)
        } else {
          throw new RuntimeException("Waiting for init completion")
        }
      }
    }

  }

  private def createPostgresDb(databaseName: String): Unit = {
    val defaultDbDatasource: HikariDataSource = {
      val masterUrl = PostgresConnection.url()
      val config = new HikariConfig()
      config.setJdbcUrl(masterUrl)
      config.setDriverClassName("org.postgresql.Driver")
      new HikariDataSource(config)
    }
    val conn = defaultDbDatasource.getConnection()
    val stmt = conn.createStatement()
    val find = conn.prepareStatement("SELECT count(*) FROM pg_database WHERE datname = ?")
    find.setString(1, databaseName)
    val count = {
      val results = find.executeQuery()
      if (results.next()) results.getInt(1) else 0
    }
    if (count <= 0) {
      stmt.execute(s"CREATE DATABASE $databaseName")
    }
    conn.close()
    defaultDbDatasource.close()
  }

  private def createDbManager(databaseName: String): DatabaseManager = {
    DatabaseManager.create(
      databaseConnectionConfig = dbConnectionConfig(databaseName),
      migrationTarget = migrationTarget,
      disableFlywayHistoryClean = false
    )
  }

  private def dbConnectionConfig(databaseName: String): DatabaseManager.DatabaseConnectionConfig =
    DatabaseManager.DatabaseConnectionConfig(PostgresConnection.url(databaseName), numThreads = Some(2), queueSize = Some(2))

  private def executeFlywayMigration(database: DatabaseManager): Unit = {
    val start = System.currentTimeMillis()
    database.clear()
    database.migrate()
    val delta = System.currentTimeMillis() - start
    println(s"[${this.suiteName.toLowerCase}] executeFlywayMigration for '$dbName' finished in ${delta}ms")
  }

  private def spawnApp(): RunnableApplication = {
    val jdbcUrl = PostgresConnection.url(dbName)
    val testConfig = config
      .withValue("database.jdbc-url", ConfigValueFactory.fromAnyRef(jdbcUrl))
    val databaseConfig = dbConnectionConfig(dbName)

    createTestInstance(testConfig, databaseConfig)
  }

  private def createTestInstance(config: Config, databaseConfig: DatabaseManager.DatabaseConnectionConfig) = new RunnableApplication(
    config = config,
    actorSystemName = config.getString("application.actor-system-name"),
    bindInterface = "0.0.0.0",
    bindPort = 0, // to avoid port conflicts during testing only
    databaseConfig = databaseConfig,
  )

  private def currentUri: Uri = {
    Uri(scheme = "http", appBinding.getHostName, appBinding.getPort)
  }

  private def resetMountebankRequests(): Unit = {
    // reset all mountebanks requests state e.g
    // mountebankProxyAbc.resetMountebankUri(...)
  }

  def getServiceStatus(): Future[StatusResponse] = httpClient.get[StatusResponse](path = "/status", headers = Seq.empty)

  def getDocs(): Future[String] = httpClient.getWithRawResponseBody(path = "/docs", headers = Seq.empty)

}
