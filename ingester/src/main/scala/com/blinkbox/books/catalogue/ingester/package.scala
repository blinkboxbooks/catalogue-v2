package com.blinkbox.books.catalogue

package object ingester {
  lazy val jvalue = {
    import org.json4s.JsonDSL._
    ("distribute" -> true) ~
    ("title" -> "Little park bear") ~
    ("availability" ->
      ("available" -> true) ~
        ("code" -> "PB") ~
        ("extra" -> "Bear cub")) ~
    ("isbn" -> "9780007236893") ~
    ("regionalRights" ->
      ("GB" -> true) ~
        ("WORLD" -> true) ~
        ("ROW" -> false)) ~
    ("prices" -> List(
      ("currency" -> "GBP") ~
        ("isAgency" -> true) ~
        ("amount" -> 1.23) ~
        ("includesTax" -> true) ~
        ("tax" -> ("amount" -> 1.00)))) ~
    ("publisher" -> "PamPublish") ~
    ("media" ->
      ("images" -> List(
        ("classification" -> List(
          ("realm" -> "type") ~
            ("id" -> "front_cover"))) ~
          ("uris" -> List(
            ("type" -> "resource_server") ~
              ("uri" -> "https://media.blinkboxbooks.com/path/to/file.png"),
            ("type" -> "static") ~
              ("uri" -> "http://container.azure.com/path/to/file.jpg"),
            ("type" -> "static") ~
              ("uri" -> "http://container.azure.com/path/to/file.jpg") ~
              ("params" -> "img:m=scale;img:w=300;v=0"))) ~
          ("width" -> 1200) ~
          ("height" -> 2500) ~
          ("size" -> 25485))) ~
        ("epubs" -> List(
          ("classification" -> List(
            ("realm" -> "type") ~
              ("id" -> "full_bbbdrm"))) ~
            ("uris" -> List(
              ("type" -> "static") ~
                ("uri" -> "http://container.azure.com/path/to/file.epub"),
              ("type" -> "resource_server") ~
                ("uri" -> "https://media.blinkboxbooks.com/path/to/file.epub"))) ~
            ("keyfile" -> "https://keys.blinkboxbooks.com/path/to/keyfile.epub.9780111222333.key") ~
            ("wordCount" -> 37462) ~
            ("size" -> 25485),
          ("classification" -> List(
            ("realm" -> "type") ~
              ("id" -> "sample"))) ~
            ("uris" -> List(
              ("type" -> "resource_server") ~
                ("uri" -> "https://media.blinkboxbooks.com/path/to/file.sample.epub"))) ~
            ("wordCount" -> 3746) ~
            ("size" -> 2548)))) ~
    ("languages" -> List("eng")) ~
    ("descriptions" -> List(
      ("classification" -> List(
        ("realm" -> "Test1") ~
          ("id" -> "ID1"),
        ("realm" -> "Test2") ~
          ("id" -> "ID2"))) ~
        ("content" -> "This is a simple description of this book ... or something like that.") ~
        ("type" -> "45") ~
        ("author" -> "TheAuthhhhor"))) ~
    ("subjects" -> List(
      ("type" -> "BISAC") ~
      ("code" -> "FIC050000"))) ~
    ("prices" -> List(
      ("amount" -> 10.0) ~
      ("currency" -> "GBP") ~
      ("includesTax" -> false) ~
      ("isAgency" -> false))) ~
    ("contributors" -> List(
      ("role" -> "Author") ~
      ("id" -> "metheauthor") ~
      ("displayName" -> "displayName1") ~
      ("sortName" -> "SortName1")))
  }
}
