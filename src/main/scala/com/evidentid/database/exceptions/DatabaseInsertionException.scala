package com.evidentid.database.exceptions

sealed class DatabaseInsertionException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

final class DuplicateInsertionException(message: String, cause: Option[Throwable] = None) extends DatabaseInsertionException(message, cause)
