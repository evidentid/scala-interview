package com.evidentid.application.test

import akka.actor
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, TestSuite}

trait ActorSystemFixture extends TestSuite with BeforeAndAfterAll with ScalaFutures { thisSuite =>

  implicit var classicActorSystem: actor.ActorSystem = _

  var testKit: ActorTestKit = _
  var actorSystem: ActorSystem[_] = _
  var http: HttpExt = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    testKit = ActorTestKit()
    actorSystem = testKit.system
    classicActorSystem = actorSystem.classicSystem
    http = Http()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    http.shutdownAllConnectionPools().futureValue
    testKit.shutdownTestKit()
  }

}
