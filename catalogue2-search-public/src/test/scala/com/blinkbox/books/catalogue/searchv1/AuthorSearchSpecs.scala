package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.Contributor
import com.blinkbox.books.catalogue.common.{BookFixtures, Events}
import com.blinkbox.books.catalogue.searchv1.V1SearchService.BookSearchResponse
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
    catalogueIndex indexAndCheck(b1, b2, b3, b4) andAwaitFor 10.seconds
  }

  def checkQuery(q: String)(f: Iterable[String] => Unit) =
    Get(s"/catalogue/search/books?q=$q") ~> routes ~> check {
      status should equal(StatusCodes.OK)
      f(responseAs[BookSearchResponse].books.getOrElse(Nil).map(_.id))
    }

  def shouldEqual(expected: Iterable[Events.Book]): Iterable[String] => Unit =
    bs => bs should equal(expected.map(_.isbn))

  "Searching by author the API" should "return close matches after the exact ones" in {
    checkQuery("Kilgore%20Trout")(shouldEqual(b1 :: b2 :: Nil))
    checkQuery("Kilgore%20Vonnegut") { bs =>
      bs.head should equal(b2.isbn)
      bs.tail should contain theSameElementsAs (b1 :: b3 :: Nil).map(_.isbn)
    }
    checkQuery("Kurt%20Vonnegut")(shouldEqual(b3 :: b2 :: Nil))
  }

  it should "return only exact matches if there is nothing in common with other authors" in {
    checkQuery("Charles%20Dickens")(shouldEqual(b4 :: Nil))
  }
}
