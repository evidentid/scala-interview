package com.evidentid

import akka.actor.testkit.typed.scaladsl.LogCapturing
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}

import java.util.UUID
import scala.concurrent.ExecutionContext

/** Base trait for unit test.
  *
  * Please extend @link UnitSpec} if possible, as it might improve compilation time compared to mixing-in a trait.
  */
trait UnitSpecLike
    extends AnyFlatSpec
    with BeforeAndAfter
    with BeforeAndAfterAll
    with Inside
    with EitherValues
    with LogCapturing
    with Matchers
    with MockFactory
    with OptionValues
    with ScalaFutures
    with TableDrivenPropertyChecks
    with LoneElement {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(50, Millis)))

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

  def nextId(): UUID = UUID.randomUUID()

}
