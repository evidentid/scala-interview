package com.evidentid.application

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown, Terminated}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.server.Route
import com.evidentid.database.DatabaseManager
import com.evidentid.logging.Logging

import java.net.InetSocketAddress
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

trait Application extends Logging {
  val system: ActorSystem
  def start(): Future[Terminated]

  def terminate(): Future[Done] = CoordinatedShutdown(system).run(Application.UserInitiatedShutdown)

  def startHttpServer(http: HttpExt, bindInterface: String, bindPort: Int, appRoutes: Route)(
    system: ActorSystem
  ): Future[InetSocketAddress] = {
    implicit def dispatcher: ExecutionContextExecutor = system.dispatcher
    implicit val classicSystem: ActorSystem = system.classicSystem

    addConnectionPoolShutdownTask(http, system)
    val bindingFuture = http
      .newServerAt(bindInterface, bindPort)
      .bind(appRoutes)
      .map { server =>
        server.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds)
      }
    bindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
        address
      case Failure(ex) =>
        logger.error("Failed to bind HTTP endpoint, terminating system", ex)
        CoordinatedShutdown(system).run(Application.UserInitiatedShutdown)
    }
    bindingFuture.map(_.localAddress)
  }

  def addDatabaseCoordinatedShutdownTask(databaseManager: DatabaseManager)(implicit system: ActorSystem): Unit = {
    CoordinatedShutdown(system)
      .addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "closeDatabaseConnection") { () =>
        Future {
          system.log.info("Shutting down database connection.")
          databaseManager.close()
          Done
        }(system.dispatcher)
      }
  }

  private def addConnectionPoolShutdownTask(http: HttpExt, system: ActorSystem): Unit = {
    CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "shutdownAllConnectionPools") { () =>
      system.log.info("Shutting down connection pools.")
      http.shutdownAllConnectionPools().map(_ => Done)(system.dispatcher)
    }
  }

}

object Application {
  case object UserInitiatedShutdown extends CoordinatedShutdown.Reason
}
