package com.blinkbox.books.catalogue.searchv1

import com.blinkbox.books.catalogue.common.{ BookFixtures, Contributor, OtherText, Subject, Events }
import com.blinkbox.books.catalogue.searchv1.V1SearchService.{BookSimilarResponse, Book}
import org.scalatest.{ FlatSpec, Matchers }
import spray.http.StatusCodes

import scala.concurrent.duration._
import scala.util.Random

class SimilarBooksSpecs extends FlatSpec with Matchers with ApiSpecBase {

  val baseBook = BookFixtures.simpleBook

  def book(
    isbn: String,
    title: String,
    authors: List[String] = baseBook.contributors.map(_.displayName),
    description: String = baseBook.descriptions.headOption.map(_.content).getOrElse(""),
    subjects: List[String] = baseBook.subjects.map(_.code)
  ) =
    baseBook.copy(
      isbn = isbn,
      title = title,
      contributors = authors.map(n => Contributor(Random.nextString(10), "author", n, n)),
      descriptions = OtherText(Nil, description, "description", None) :: Nil,
      subjects = subjects.map(c => Subject("bisac", c, Some(true)))
    )

  val alpha = book("0000000000001", "Alpha", "Kilgore Trout" :: Nil, "Book about something", "abc123" :: "def456" :: Nil)
  val beta = book("0000000000002", "Beta", "Luther Blissett" :: Nil, "Anything really does", "ghi789" :: Nil)
  val gamma = book("0000000000003", "Gamma", "Kilgore Trout" :: Nil, "Foobar bar baz", "jkl000" :: Nil)
  val delta = book("0000000000004", "Delta", "Bilbo Baggins" :: Nil, "Anything", "zzz999" :: Nil)
  val aNewAlpha = book("0000000000005", "A new Alpha", "Anonymous" :: Nil, "Running out of stuffs to write", "yyy888" :: Nil)
  val epsilon = book("0000000000006", "Epsilon", "Soumynona" :: Nil, "Blablabla", "xxx777" :: "zzz999" :: Nil)

  val dataSet = alpha :: beta :: gamma :: delta :: aNewAlpha :: epsilon :: Nil

  override def beforeAll(): Unit = {
    super.beforeAll()
    catalogueIndex indexAndCheck (dataSet.toSeq: _*) andAwaitFor (10.seconds)
  }

  "The similar-books endpoint" should "return books that are similar to the provided one for title, description, author or subject" in {
    def testSimilar(isbn: String)(f: Seq[String] => Unit) = {
      Get(s"/catalogue/search/books/$isbn/similar") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        f(responseAs[BookSimilarResponse].books.getOrElse(Seq.empty).map(_.id))
      }
    }

    def isbns(books: Events.Book*) = books.map(_.isbn)

    testSimilar(alpha.isbn) { _ should contain theSameElementsAs isbns(gamma, aNewAlpha) }
    testSimilar(beta.isbn) { _ should contain theSameElementsAs isbns(delta) }
    testSimilar(gamma.isbn) { _ should contain theSameElementsAs isbns(alpha) }
    testSimilar(delta.isbn) { _ should contain theSameElementsAs isbns(beta, epsilon) }
    testSimilar(aNewAlpha.isbn) { _ should contain theSameElementsAs isbns(alpha) }
    testSimilar(epsilon.isbn) { _ should contain theSameElementsAs isbns(delta) }
  }

  it should "return a 400 signaling an invalid ID if the isbn is not numeric" in {
    Get("/catalogue/search/books/abcdefghijklm/similar") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse("Invalid ID: abcdefghijklm")
    }
  }

  it should "return a 400 signaling an invalid ID if the isbn is shorter than 13 digits" in {
    Get("/catalogue/search/books/123456789012/similar") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse("Invalid ID: 123456789012")
    }
  }

  it should "return a 400 signaling an invalid ID if the isbn is longer than 13 digits" in {
    Get("/catalogue/search/books/12345678901234/similar") ~> routes ~> check {
      status should equal(StatusCodes.BadRequest)
      checkInvalidResponse("Invalid ID: 12345678901234")
    }
  }
}
