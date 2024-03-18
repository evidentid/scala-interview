package com.evidentid.database.exceptions

sealed abstract class EntityResolutionException(msg: String, cause: Option[Throwable] = None) extends Exception(msg, cause.orNull)

final class EntityNotFoundException(msg: String, cause: Option[Throwable] = None) extends EntityResolutionException(msg, cause)
