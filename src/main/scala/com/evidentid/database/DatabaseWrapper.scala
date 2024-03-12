package com.evidentid.database

import com.evidentid.logging.Logging
import slick.basic.DatabasePublisher
import slick.dbio.{DBIOAction, NoStream, Streaming}
import slick.jdbc.PostgresProfile.api.Database

import scala.concurrent.{ExecutionContext, Future}

class DatabaseWrapper(database: Database) extends Logging {

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] =
    database.run(a)

  final def stream[T](a: DBIOAction[_, Streaming[T], Nothing]): DatabasePublisher[T] = database.stream(a)

  def close(): Unit = database.close()

  def ioExecutionContext: ExecutionContext = database.ioExecutionContext
}
