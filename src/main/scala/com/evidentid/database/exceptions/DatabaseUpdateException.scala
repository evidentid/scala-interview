package com.evidentid.database.exceptions

class DatabaseUpdateException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)
