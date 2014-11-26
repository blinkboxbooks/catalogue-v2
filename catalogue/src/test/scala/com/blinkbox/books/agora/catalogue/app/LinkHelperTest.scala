package com.blinkbox.books.agora.catalogue.app

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpecLike, Matchers}
import com.blinkbox.books.test.MockitoSyrup
import spray.http.Uri

@RunWith(classOf[JUnitRunner])
class LinkHelperTest extends FlatSpecLike with Matchers with MockitoSyrup {
  val helper = new LinkHelper(Uri("test"), "/contributor", "/publisher", "/pricing", "/book", "/book/{isbn}/synopsis")
  
  it should "construct a link to a given book" in {
    val link = helper.linkForBook("isbn")
    link.rel shouldEqual "urn:blinkboxbooks:schema:book"
    link.href shouldEqual "test/book/isbn"
    link.title shouldEqual Some("Book")
    link.targetGuid shouldEqual Some("urn:blinkboxbooks:id:book:isbn")
  }
  
  it should "construct a link to the contributor " in {
    val link = helper.linkForContributor("123", "name")
    link.rel shouldEqual "urn:blinkboxbooks:schema:contributor"
    link.href shouldEqual "test/contributor/123"
    link.title shouldEqual Some("name")
    link.targetGuid shouldEqual Some("urn:blinkboxbooks:id:contributor:123")
  }
  
  it should "construct an image link to the contributor photo" in {
    val link = helper.imageLinkForContributor("photo")
    link.rel shouldEqual "urn:blinkboxbooks:image:contributor"
    link.href shouldEqual "photo"
    link.title shouldEqual Some("Image for contributor")
    link.targetGuid shouldEqual None
  }

  it should "construct a link to the books for the contributor " in {
    val link = helper.linkForContributorBooks("123")
    link.rel shouldEqual "urn:blinkboxbooks:schema:books"
    link.href shouldEqual "test/book?contributor=123"
    link.title shouldEqual Some("Books for contributor")
    link.targetGuid shouldEqual None
  }

  it should "construct a link to the publisher" in {
    val link = helper.linkForPublisher(42, "publisher")
    link.rel shouldEqual "urn:blinkboxbooks:schema:publisher"
    link.href shouldEqual "test/publisher/42"
    link.title shouldEqual Some("publisher")
    link.targetGuid shouldEqual Some("urn:blinkboxbooks:id:publisher:42")
  }  

  it should "construct a link to books for the publisher" in {
    val link = helper.linkForPublisherBooks(42)
    link.rel shouldEqual "urn:blinkboxbooks:schema:books"
    link.href shouldEqual "test/book?publisher=42"
    link.title shouldEqual Some("Books for publisher")
    link.targetGuid shouldEqual None
  }  

  it should "construct a link to the book synopsis" in {
    val link = helper.linkForBookSynopsis("isbn")
    link.rel shouldEqual "urn:blinkboxbooks:schema:synopsis"
    link.href shouldEqual "test/book/isbn/synopsis"
    link.title shouldEqual Some("Synopsis")
    link.targetGuid shouldEqual Some("urn:blinkboxbooks:id:synopsis:isbn")
  }  

  it should "construct a link to the book prices" in {
    val link = helper.linkForBookPricing("isbn")
    link.rel shouldEqual "urn:blinkboxbooks:schema:bookpricelist"
    link.href shouldEqual "test/pricing?book=isbn"
    link.title shouldEqual Some("Price")
    link.targetGuid shouldEqual None
  }  

  it should "construct a link to the sample" in {
    val link = helper.linkForSampleMedia("url")
    link.rel shouldEqual "urn:blinkboxbooks:schema:samplemedia"
    link.href shouldEqual "url"
    link.title shouldEqual Some("Sample")
    link.targetGuid shouldEqual None
  }  
}
