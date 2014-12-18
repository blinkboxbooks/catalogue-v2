package com.blinkbox.books.catalogue.common.search

case class CommunicationException(cause: Throwable) extends RuntimeException(cause)
