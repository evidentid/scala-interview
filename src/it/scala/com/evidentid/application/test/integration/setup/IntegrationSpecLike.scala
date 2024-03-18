package com.evidentid.application.test.integration.setup

import akka.actor.testkit.typed.scaladsl.LogCapturing
import com.evidentid.application.test.ConfigAware
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID

/** Base trait for integration test.
  *
  * Please extend {@link IntegrationSpec} if possible, as it might improve compilation time compared to mixing-in a trait.
  */
trait IntegrationSpecLike
    extends AnyFlatSpec
    with BeforeAndAfter
    with BeforeAndAfterAll
    with DatabaseFixture
    with EitherValues
    with Eventually
    with Inside
    with LogCapturing
    with Matchers
    with OptionValues
    with ScalaFutures
    with TableDrivenPropertyChecks
    with LoneElement
    with ConfigAware {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override val clearLogsAfterEachTest: Boolean = true

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(5, Millis)))

  override protected def beforeAll(): Unit = {
    val start = System.currentTimeMillis()
    super.beforeAll()
    val delta = System.currentTimeMillis() - start
    println(s"[${this.suiteName.toLowerCase}] IntegrationSpecLike beforeAll finished in ${delta}ms")
  }

  def randomize(str: String) = s"${str}_${UUID.randomUUID().toString}"
}
