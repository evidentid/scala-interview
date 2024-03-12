package com.evidentid.application.test

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scala.sys.process._
import scala.util.Using

object PostgresConnection {

  def user: String = {
    Using(scala.io.Source.fromResource("docker/testbed_default_user.txt"))(_.mkString).get
  }

  def password: String = {
    Using(scala.io.Source.fromResource("docker/testbed_default_passwd.txt"))(_.mkString).get
  }

  def currentBinding: String = {
    "docker-compose port postgres 5432".!!.trim()
  }

  // default value is name of DB that is available in our docker image out of the box
  def url(dbName: String = "evident"): String = {
    val binding = currentBinding.replace("0.0.0.0", "localhost")

    s"jdbc:postgresql://$binding/$dbName?user=$user&password=$password"
  }

  def createDataSource: HikariDataSource = {
    val config = new HikariConfig()
    val postgresUrl = url()
    config.setJdbcUrl(postgresUrl)
    new HikariDataSource(config)
  }

  def isolatedUrl(testName: String): String = {
    val binding = currentBinding.replace("0.0.0.0", "localhost")

    s"jdbc:postgresql://$binding/$testName?user=$user&password=$password"
  }

  val productName = "PostgreSQL"
  val MAJOR_VERSION = 11
}
