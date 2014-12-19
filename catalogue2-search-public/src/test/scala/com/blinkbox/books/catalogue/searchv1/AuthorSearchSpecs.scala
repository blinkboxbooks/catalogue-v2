
package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.Contributor
import com.blinkbox.books.catalogue.common.{BookFixtures, Events}
import com.blinkbox.books.catalogue.searchv1.V1SearchService.{Book, BookSearchResponse, BookSimilarResponse, BookCompletionResponse}
import com.blinkbox.books.spray.v1.Link
import com.sksamuel.elastic4s.ElasticDsl
import org.json4s.Extraction
import org.json4s.JsonAST._
import org.scalatest.{FlatSpec, Matchers}
import scala.util.Random
import spray.http.StatusCodes

import scala.concurrent.duration._

class AuthorSearchSpecs extends FlatSpec with Matchers with ApiSpecBase {

  val b = BookFixtures.simpleBook

  def withAuthor(name: String) =
    b.copy(
      isbn = Random.nextInt(200000).toString,
      contributors = Contributor("author", Random.nextInt(20000).toString, name, name) :: Nil)

  val b1 = withAuthor("Kilgore Trout")
  val b2 = withAuthor("Kilgore Vonnegut")
  val b3 = withAuthor("Kurt Vonnegut")
  val b4 = withAuthor("Charles Dickens")

  override def beforeAll(): Unit = {
    super.beforeAll()
    catalogueIndex indexAndCheck(b1, b2, b3, b4) andAwaitFor (10.seconds)
  }

  def checkQuery(q: String, expected: Iterable[Events.Book]) =
    Get(s"/catalogue/search/books?q=$q") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      val books = responseAs[BookSearchResponse].books.getOrElse(Nil)

      books.toList.map(_.id) should equal(expected.toList.map(_.isbn))
    }

  "Searching by author the API" should "return close matches after the exact ones" in {
    checkQuery("Kilgore%20Trout", b1 :: b2 :: Nil)
    checkQuery("Kilgore%20Vonnegut", b2 :: b1 :: b3 :: Nil)
    checkQuery("Kurt%20Vonnegut", b3 :: b2 :: Nil)
  }

  it should "return only exact matches if there is nothing in common with other authors" in {
    checkQuery("Charles%20Dickens", b4 :: Nil)
  }
}
