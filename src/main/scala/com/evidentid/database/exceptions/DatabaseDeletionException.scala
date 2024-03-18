package com.evidentid.database.exceptions

sealed class DatabaseDeletionException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

final class UnexpectedDeletionCountException(message: String, cause: Option[Throwable] = None)
    extends DatabaseDeletionException(message, cause)
