package com.blinkbox.books.agora.catalogue.contributor

import com.blinkbox.books.spray.v1.Link
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class Contributor(guid: String, id: String, displayName: String, sortName: String,
                       bookCount: Int, biography: String, links: List[Link])

case class ContributorGroup(guid: String, id: String, nameKey: String, displayName: String, startDate: String,
                            endDate: String, enabled: String, contributors: List[ContributorRef])

/** A Contributor reference used in ContributorGroup */
// TODO: Revisit this -- maybe it's better just to use Contributor.
case class ContributorRef(guid: String, id: String)

object Contributor {
  def apply(id: String, displayName: String, sortName: String, bookCount: Int, biography: String, links: List[Link]) =
    new Contributor(s"urn:blinkboxbooks:id:contributor:$id", id, displayName, sortName, bookCount, biography, links)
}

object ContributorGroup {
  val fmt = DateTimeFormat.forPattern("yyyy-MM-dd")
  def apply(id: String, nameKey: String, displayName: String, startDate: DateTime, endDate: DateTime, enabled: Boolean,
            contributors: List[ContributorRef]) = new ContributorGroup(
    s"urn:blinkboxbooks:id:contributorgroup:$id", id, nameKey, displayName, fmt.print(startDate), fmt.print(endDate), enabled.toString, contributors)
}

object ContributorRef {
  def apply(id: String) = new ContributorRef(s"urn:blinkboxbooks:id:contributor:$id", id)
}
