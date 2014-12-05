package com.blinkbox.books.agora.catalogue.app

import com.blinkbox.books.spray.v1.Link
import spray.http.Uri
import spray.http.Uri.{Authority, Path}

class LinkHelper(val externalUrl: Uri, val contributorPath: String, publisherPath: String, pricingPath: String, val bookPath: String, synopsisPath: String) {
  def linkForBook(isbn: String) = Link(
    "urn:blinkboxbooks:schema:book",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(s"$bookPath/$isbn"))).toString(),
    Some("Book"),
    Some(s"urn:blinkboxbooks:id:book:$isbn")
  )

  def linkForPublisherBooks(publisherId: Long) = Link(
    "urn:blinkboxbooks:schema:books",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(bookPath)).withQuery(("publisher", publisherId.toString))).toString(),
    Some("Books for publisher")
  )

  def linkForContributorBooks(contributorId: String) = Link(
    "urn:blinkboxbooks:schema:books",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(bookPath)).withQuery(("contributor", contributorId))).toString(),
    Some("Books for contributor")
  )

  def linkForCategoryBooks(categoryId: Long) = Link(
    "urn:blinkboxbooks:schema:books",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(bookPath)).withQuery(("category", categoryId.toString))).toString(),
    Some("Books for category")
  )

  def imageLinkForContributor(photoUrl: String) = Link(
    "urn:blinkboxbooks:image:contributor",
    photoUrl,
    Some("Image for contributor")
  )

  def linkForPublisher(publisherId: Long, publisherName: String) = Link(
    "urn:blinkboxbooks:schema:publisher",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(s"$publisherPath/$publisherId"))).toString(),
    Some(publisherName),
    Some(s"urn:blinkboxbooks:id:publisher:$publisherId")
  )

  def linkForContributor(contributorId: String, contributorName: String) = Link(
    "urn:blinkboxbooks:schema:contributor",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(s"$contributorPath/$contributorId"))).toString(),
    Some(contributorName),
    Some(s"urn:blinkboxbooks:id:contributor:$contributorId")
  )

  def linkForBookSynopsis(isbn: String) = Link(
    "urn:blinkboxbooks:schema:synopsis",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(synopsisPath.replace("{isbn}", isbn)))).toString(),
    Some("Synopsis"),
    Some(s"urn:blinkboxbooks:id:synopsis:$isbn")
  )

  def linkForBookPricing(isbn: String) = Link(
    "urn:blinkboxbooks:schema:bookpricelist",
    makeRelative(externalUrl.withPath(externalUrl.path ++ Path(pricingPath)).withQuery(("book", isbn))).toString(),
    Some("Price")
  )

  def linkForSampleMedia(sampleUrl: String) = Link(
    "urn:blinkboxbooks:schema:samplemedia",
    sampleUrl,
    Some("Sample")
  )

  private def makeRelative(uri: Uri): Uri = {
    uri.copy(scheme = "", authority = Authority.Empty)
  }
}

object LinkHelper {
  import com.blinkbox.books.spray.url2uri
  def apply(cfg: AppConfig) = new LinkHelper(
    cfg.service.externalUrl,
    cfg.contributor.path,
    cfg.publisher.path,
    cfg.price.path,
    cfg.book.path,
    cfg.book.synopsisPathLink
  )
}
