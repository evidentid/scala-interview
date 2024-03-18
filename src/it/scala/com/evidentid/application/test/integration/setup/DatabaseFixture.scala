package com.evidentid.application.test.integration.setup

import akka.actor.testkit.typed.scaladsl.LogCapturing
import com.evidentid.application.test.{ConfigAware, PostgresConnection}
import com.evidentid.database.{DatabaseManager, DatabaseWrapper}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite, TestSuite}

import java.util.concurrent.atomic.AtomicBoolean

trait DatabaseFixture extends TestSuite with BeforeAndAfterEach with BeforeAndAfterAll with LogCapturing with ConfigAware with Eventually {

  this: Suite =>

  val requireSeparateDb: Boolean

  val dbName: String = if (requireSeparateDb) this.suiteName.toLowerCase else "eid_application_integration_tests"
  val dbUrl: String = PostgresConnection.isolatedUrl(dbName)

  def migrationTarget: String = "latest"

  lazy val dataSource: HikariDataSource = {
    val masterUrl = PostgresConnection.url()
    val config = new HikariConfig()
    config.setJdbcUrl(masterUrl)
    config.setDriverClassName("org.postgresql.Driver")
    new HikariDataSource(config)
  }

  lazy val databaseManager: DatabaseManager =
    DatabaseManager.create(
      databaseConnectionConfig = dbConnectionConfig(dbUrl),
      migrationTarget = migrationTarget,
      disableFlywayHistoryClean = false
    )

  lazy val database: DatabaseWrapper = databaseManager.database

  private def setupSuiteDatabase(): Unit = {
    val conn = dataSource.getConnection
    val stmt = conn.createStatement()
    val find = conn.prepareStatement("SELECT count(*) FROM pg_database WHERE datname = ?")
    find.setString(1, dbName)
    val count = {
      val results = find.executeQuery()
      if (results.next()) results.getInt(1) else 0
    }
    if (requireSeparateDb || DatabaseFixture.IsSharedDbInitialisationStarted.compareAndSet(false, true)) {
      println(s"[${this.suiteName.toLowerCase}] Starting DatabaseFixture initialisation for '$dbName'")
      if (count <= 0) {
        stmt.execute(s"CREATE DATABASE $dbName")
      }
      executeFlywayMigration()
      DatabaseFixture.IsSharedDbAlreadyInitialised.set(true)
      println(s"[${this.suiteName.toLowerCase}] Finished DatabaseFixture initialisation for '$dbName'")
    } else {
      println(s"[${this.suiteName.toLowerCase}] Shared DB initialisation already in progress, waiting for completion")
      eventually(timeout(scaled(Span(60, Seconds))), interval(scaled(Span(200, Millis)))) {
        if (!DatabaseFixture.IsSharedDbAlreadyInitialised.get()) {
          throw new RuntimeException(s"Waiting for '$dbName' DB init completion")
        }
      }
    }
    conn.close()
  }

  private def executeFlywayMigration(): Unit = {
    val start = System.currentTimeMillis()
    databaseManager.clear()
    databaseManager.migrate()
    val delta = System.currentTimeMillis() - start
    println(s"[${this.suiteName.toLowerCase}] DatabaseFixture executeFlywayMigration for '$dbName' finished in ${delta}ms")
  }

  private def dbConnectionConfig(dbUrl: String): DatabaseManager.DatabaseConnectionConfig =
    DatabaseManager.DatabaseConnectionConfig(dbUrl, numThreads = Some(2), queueSize = Some(2))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    clearCapturedLogs()
    setupSuiteDatabase()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    databaseManager.close()
    dataSource.close()
  }

}

object DatabaseFixture {
  val IsSharedDbInitialisationStarted: AtomicBoolean = new AtomicBoolean(false)
  val IsSharedDbAlreadyInitialised: AtomicBoolean = new AtomicBoolean(false)
}
