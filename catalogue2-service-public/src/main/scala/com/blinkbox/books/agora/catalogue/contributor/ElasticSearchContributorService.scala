package com.blinkbox.books.agora.catalogue.contributor

import java.util.concurrent.Executors
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.{ExecutionContext, Future}

class ElasticSearchContributorService extends ContributorService with StrictLogging {
  implicit val executionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutor(Executors.newCachedThreadPool))

  override def getContributorById(id: String): Future[Option[Contributor]] = {
    Future.successful(Some(Contributor(id, id, "name", "sort-name", 42, "bio", List())))
  }
}
