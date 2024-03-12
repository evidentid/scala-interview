package com.evidentid.logging

import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec

trait Logging {
  final val logger: Logger = LoggerFactory.getLogger(getClass)
}

object Logging {

  /** Gets message from throwable and all its nested causes */
  def getFailureReason(throwable: Throwable): String = {
    @tailrec
    def getFailureReasonRec(t: Throwable, acc: String, currentDepth: Int, maxDepth: Int = 5): String = {
      Option(t.getCause) match {
        case Some(cause) if maxDepth == currentDepth =>
          s"$acc caused by: ${cause.getMessage}"
        case Some(cause) =>
          getFailureReasonRec(cause, s"$acc, caused by: ${cause.getClass.getName}: ${cause.getMessage}", currentDepth + 1, maxDepth)
        case None => acc
      }
    }
    Option(throwable) match {
      case None => "null"
      case Some(t) =>
        getFailureReasonRec(t, acc = s"${t.getClass.getName}: ${t.getMessage}", currentDepth = 0)
    }
  }

}
