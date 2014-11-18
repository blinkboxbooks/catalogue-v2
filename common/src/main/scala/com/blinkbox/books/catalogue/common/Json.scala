package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.IndexEntities.{SuggestionItem, SuggestionPayload, SuggestionType}
import org.json4s.{Extraction, CustomSerializer}
import org.json4s.JsonAST.{JField, JValue, JObject, JString}

object Json {

  private val suggestionPayloadSerializer = new CustomSerializer[SuggestionPayload](implicit format => ({
    case JObject(JField("type", JString("book")) :: JField("item", i) :: Nil) =>
      SuggestionPayload(SuggestionType.Book, i.extract[SuggestionItem.Book])
    case JObject(JField("type", JString("contributor")) :: JField("item", i) :: Nil) =>
      SuggestionPayload(SuggestionType.Contributor, i.extract[SuggestionItem.Contributor])
  }, {
    case SuggestionPayload(SuggestionType.Book, i) => JObject(JField("book", Extraction.decompose(i)))
    case SuggestionPayload(SuggestionType.Contributor, i) => JObject(JField("contributor", Extraction.decompose(i)))
  }))

  implicit val formats = (org.json4s.DefaultFormats ++
    com.blinkbox.books.json.DefaultFormats.customSerializers +
    suggestionPayloadSerializer)

}
