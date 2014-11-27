package com.blinkbox.books.catalogue.ingester.v1.parser

import scala.io.Source

trait XmlV1IngestionParserFixture {
  def asString(xmlResource: String): String =
    Source.fromURL(getClass.getResource(s"/$xmlResource")).mkString

  // A regex based naive implementation of
  // node replacement within xml.
  // !!! NOTE: this *should* not be used within production code
  //           as it's not optimal.
  def removeNode(nodeName: String, content: String): String =
    content.replaceAll(s"(?s)<$nodeName.*</$nodeName>", "")
}