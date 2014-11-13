package com.blinkbox.books.agora.catalogue.contributor

import java.util.concurrent.Executors
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.v1._
import com.blinkbox.books.spray.{Page, Paging}
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class ElasticSearchContributorService extends ContributorService with StrictLogging {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))

  override def getContributorById(id: String): Future[Option[Contributor]] = {
    Future.successful(Some(Contributor(id, id, "name", "sort-name", 42, "bio", List())))
  }
}

/*
class ContributorServiceImpl(contributorService: com.blinkboxbooks.bookservices.internal.api.ContributorService,
                             linkHelper: LinkHelper, serviceBaseUrl: String) extends ContributorService with StrictLogging {

  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))

  override def getContributorsById(ids: Iterable[String]) = Future {
    val contributors = contributorService.getContributors(ids.toList)
    ListPage(contributors.size, 0, contributors.size, contributorList(contributors, linkHelper))
  }

  override def getContributorsByGroupName(groupName: String, groupParam: String, page: Page) = Future {
    val contributors = contributorService.getContributorsByGroupName(groupName)
    val links = if (contributors.size > page.count) {
      val params = Seq((groupParam, groupName))
      val paging = Paging.links(Some(contributors.size), page.offset, page.count, serviceBaseUrl, Some(params), includeSelf = false)
      Some(paging.toList.map(pageLink2Link))
    } else None
    val contributorsSlice = contributors.slice(page.offset, page.offset + page.count)
    ListPage(contributors.size, page.offset, contributorsSlice.size, contributorList(contributorsSlice, linkHelper), links)
  }

  override def getContributorById(id: String) = Future {
    contributorService.getContributors(List(id)) match {
      case list => list.headOption.map(c => model(c, linkHelper))
    }
  }
}
*/

/*
class ContributorGroupServiceImpl(contributorService: com.blinkboxbooks.bookservices.internal.api.ContributorService,
                                  linkHelper: LinkHelper, serviceBaseUrl: String) extends ContributorGroupService with StrictLogging {

  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))

  override def getGroups(page: Page) = Future {
    // TODO: We don't have enough information about the total number of results, so paging here is pointless
    val groups = contributorService.getAllContributorGroups(page.offset, page.count)
    ListPage(groups.size, page.offset, groups.size, contributorGroupList(groups))
  }

  override def getGroupById(id: Long) = Future {
    contributorService.getContributorGroup(id) match {
      case group if group != null => Some(model(group))
      case _ => None
    }
  }
}
*/
