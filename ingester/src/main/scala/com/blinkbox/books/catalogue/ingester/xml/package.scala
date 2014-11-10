package com.blinkbox.books.catalogue.ingester

import scala.util.Try

package object xml {
  trait Optionable[T, R] {
    def toOption(value: T): Option[R]
  }

  implicit object StringToOptInt extends Optionable[String, Int] {
    override def toOption(value: String): Option[Int] =
      Try(value.toInt).toOption
  }

  implicit object StringToOptDouble extends Optionable[String, Double] {
    override def toOption(value: String): Option[Double] =
      Try(value.toDouble).toOption
  }

  implicit object StringToOptString extends Optionable[String, String] {
    override def toOption(value: String): Option[String] =
      Some(value)
  }

  implicit object StringToOptBoolean extends Optionable[String, Boolean] {
    override def toOption(value: String): Option[Boolean] =
      Try(value.toBoolean).toOption
  }

  implicit class RichXmlString(value: String) {
    def opt[T](implicit optionable: Optionable[String, T]): Option[T] =
      optionable.toOption(value)
  }
}
