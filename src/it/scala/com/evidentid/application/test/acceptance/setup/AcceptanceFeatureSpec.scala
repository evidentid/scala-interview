package com.evidentid.application.test.acceptance.setup

import akka.actor.Terminated
import com.evidentid.application.RunnableApplication

import java.net.InetSocketAddress
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.concurrent.Future

class AcceptanceFeatureSpec extends AcceptanceFeatureSpecLike

object AcceptanceFeatureSpec {
  val IsAppInitStarted: AtomicBoolean = new AtomicBoolean(false)
  val IsAppAlreadyInit: AtomicBoolean = new AtomicBoolean(false)
  val App: AtomicReference[RunnableApplication] = new AtomicReference[RunnableApplication]()
  var AppBinding: AtomicReference[InetSocketAddress] = new AtomicReference[InetSocketAddress]()
  var AppShutdown: AtomicReference[Future[Terminated]] = new AtomicReference[Future[Terminated]]()
  val NumberOfSpecsInProgress: AtomicInteger = new AtomicInteger(0)
}
