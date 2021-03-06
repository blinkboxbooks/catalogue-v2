package com.blinkbox.books.catalogue.ingester.v1.parser

import org.joda.time.DateTime

import scala.util.Try

object OptionalConversions {
  trait Optionable[T, R] {
    def toOption(value: T): Option[R]
  }

  implicit object StringToOptInt extends Optionable[String, Int] {
    override def toOption(value: String): Option[Int] =
      Try(value.toInt).toOption
  }

  implicit object StringToOptLong extends Optionable[String, Long] {
    override def toOption(value: String): Option[Long] =
      Try(value.toLong).toOption
  }

  implicit object StringToOptDouble extends Optionable[String, Double] {
    override def toOption(value: String): Option[Double] =
      Try(value.toDouble).toOption
  }

  implicit object StringToOptString extends Optionable[String, String] {
    override def toOption(value: String): Option[String] =
      Try(
        if(value.isEmpty) throw new RuntimeException("empty string")
        else value
      ).toOption
  }

  implicit object StringToOptBoolean extends Optionable[String, Boolean] {
    override def toOption(value: String): Option[Boolean] =
      Try(value.toBoolean).toOption
  }

  implicit object StringToOptDate extends Optionable[String, DateTime] {
    override def toOption(value: String): Option[DateTime] =
      Try(DateTime.parse(value)).toOption
  }

  implicit class RichXmlString(value: String) {
    def opt[T](implicit optionable: Optionable[String, T]): Option[T] =
      optionable.toOption(value)
  }
}
