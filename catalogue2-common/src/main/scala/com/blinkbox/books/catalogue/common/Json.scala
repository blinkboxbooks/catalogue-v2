package com.blinkbox.books.catalogue.common

import com.blinkbox.books.catalogue.common.IndexEntities.{SuggestionItem, SuggestionPayload, SuggestionType}
import org.json4s.{Extraction, CustomSerializer}
import org.json4s.JsonAST.{JField, JObject, JString}
import spray.httpx.Json4sJacksonSupport

trait Serializers {
  protected val suggestionPayloadSerializer = new CustomSerializer[SuggestionPayload](implicit format => ({
    case JObject(JField("type", JString("book")) :: JField("item", i) :: Nil) =>
      SuggestionPayload(SuggestionType.Book, i.extract[SuggestionItem.Book])
    case JObject(JField("type", JString("contributor")) :: JField("item", i) :: Nil) =>
      SuggestionPayload(SuggestionType.Contributor, i.extract[SuggestionItem.Contributor])
  }, {
    case SuggestionPayload(SuggestionType.Book, i) =>
      JObject(JField("type", JString("book")), JField("item", Extraction.decompose(i)))
    case SuggestionPayload(SuggestionType.Contributor, i) =>
      JObject(JField("type", JString("contributor")), JField("item", Extraction.decompose(i)))
  }))
}

object Json extends Json4sJacksonSupport with Serializers {

  override implicit val json4sJacksonFormats =
    com.blinkbox.books.json.DefaultFormats ++
    com.blinkbox.books.elasticsearch.client.Formats.all +
    suggestionPayloadSerializer
}
