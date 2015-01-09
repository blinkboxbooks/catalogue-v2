package com.blinkbox.books.catalogue.common.e2e

import com.blinkbox.books.elasticsearch.client.AcknowledgedResponse
import com.blinkbox.books.elasticsearch.client.{ElasticRequest, SprayElasticClient}
import com.sksamuel.elastic4s.DeleteIndexDefinition
import java.util.NoSuchElementException
import com.blinkbox.books.catalogue.common.DistributeContent
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common.search.{BulkItemResponse, Indexer, EsIndexer, HttpEsIndexer, Successful}
import com.blinkbox.books.test.FailHelper
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import spray.httpx.unmarshalling.FromResponseUnmarshaller

object Blank
trait E2EContext[+T] {
  def state: T
  def indexer: Indexer
  def withState[S](s: S): E2EContext[S]
  def delete(idx: String): Future[Unit]
  def flush(idx: String = "_all"): Future[Unit]
  def create(d: CreateIndexDefinition): Future[AcknowledgedResponse]
}

case class E2ETransportContext[+T](client: ElasticClient, indexer: EsIndexer, state: T)(implicit ec: ExecutionContext) extends E2EContext[T] {
  import com.sksamuel.elastic4s.{ ElasticDsl => E }

  def withState[S](s: S): E2EContext[S] = copy(state = s)
  def delete(idx: String): Future[Unit] = client execute { E.delete index "_all" } map (_ => ())
  def flush(idx: String): Future[Unit] = client.flush() map(_ => ())
  def create(d: CreateIndexDefinition) = client execute d map (r => AcknowledgedResponse(r.isAcknowledged))
}

case class E2EHttpContext[+T](client: SprayElasticClient, indexer: HttpEsIndexer, state: T)(
  implicit ec: ExecutionContext)
    extends E2EContext[T] {

  import com.sksamuel.elastic4s.{ ElasticDsl => E }
  import com.blinkbox.books.elasticsearch.client.ElasticClientApi._
  import com.blinkbox.books.catalogue.common.Json._

  def withState[S](s: S): E2EContext[S] = copy(state = s)
  def delete(idx: String): Future[Unit] = client execute { E.delete index "_all" } map { _ => () }
  def flush(idx: String): Future[Unit] = client execute RefreshAllIndices map { _ => () }
  def create(d: CreateIndexDefinition) = client execute d
}


trait E2EDsl
  extends ScalaFutures
  with FailHelper {

  sealed trait Outcome[Out]
  object Outcome {
    case object Succeed extends Outcome[Unit]
    case class FailWith[T <: Throwable: Manifest]() extends Outcome[T] {
      def check(context: E2EContext[Future[_]])(implicit ec: ExecutionContext): T =
        failingWith[T](context.state)
    }
  }

  def using(client: ElasticClient, indexer: EsIndexer)(implicit ec: ExecutionContext) = E2ETransportContext(client, indexer, Blank)
  def using(client: SprayElasticClient, indexer: HttpEsIndexer)(implicit ec: ExecutionContext) = E2EHttpContext(client, indexer, Blank)

  implicit class CommonOps[S](context: E2EContext[S])(implicit ec: ExecutionContext) {
    def advance[T](stateFn: S => T): E2EContext[T] = context.withState(stateFn(context.state))
    def advance[T](newState: T): E2EContext[T] = advance { _: S => newState }
  }

  implicit class FutureOps[T](context: E2EContext[Future[T]])(implicit ec: ExecutionContext) {
    def andAfter(block: T => Unit): Unit = whenReady(context.state) { res => block(res) }

    def andAwaitFor(atMost: Duration): Unit = Await.ready(context.state, atMost)

    def should[Out](o: Outcome[Out]): Out = o match {
      case Outcome.Succeed => context andAfter(_ => ())
      case f: Outcome.FailWith[Out] => f.check(context)
    }
  }

  implicit class BlankOps(context: E2EContext[Blank.type])(implicit ec: ExecutionContext) {
    def createIndex(indexDef: CreateIndexDefinition): E2EContext[Future[AcknowledgedResponse]] =
      context advance (for {
        _   <- context.delete("_all")
        idx <- context.create(indexDef)
        _   <- context.flush()
      } yield idx)
  }

  implicit class WithDefinitionOps(context: E2EContext[Future[AcknowledgedResponse]])(implicit ec: ExecutionContext) {
    def index(content: DistributeContent*): E2EContext[Future[Iterable[BulkItemResponse]]] =
      context advance { state =>
        (for {
          resp    <- state if (resp.acknowledged)
          idxResp <- context.indexer index content
          _       <- context.flush()
        } yield idxResp) recover {
          case _: NoSuchElementException =>
            throw new RuntimeException("Mapping creation has not been acknowledged")
        }
      }

    def indexAndCheck(content: DistributeContent*): E2EContext[Future[Iterable[BulkItemResponse]]] = index(content: _*) ensure allSucceded
  }

  implicit class WithContentOps(context: E2EContext[Future[Iterable[BulkItemResponse]]])(implicit ec: ExecutionContext) {

    def ensure(predicate: Iterable[BulkItemResponse] => Boolean): E2EContext[Future[Iterable[BulkItemResponse]]] =
      context advance { state =>
        state filter predicate recover {
          case _: NoSuchElementException =>
            throw new RuntimeException("A checked condition failed")
        }
      }

    def andThen[T](block: => Future[T]): E2EContext[Future[T]] = context advance { state => state.flatMap(_ => block) }
  }

  val allSucceded = (items: Iterable[BulkItemResponse]) => items.forall(_.isInstanceOf[Successful])

  val succeed = Outcome.Succeed
  def failWith[T <: Throwable: Manifest] = Outcome.FailWith[T]()
}
