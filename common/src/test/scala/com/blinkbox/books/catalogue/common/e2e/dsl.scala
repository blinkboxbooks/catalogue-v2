package com.blinkbox.books.catalogue.common.e2e

import java.util.NoSuchElementException
import com.blinkbox.books.catalogue.common.Events.Book
import com.blinkbox.books.catalogue.common.search.{BulkItemResponse, EsIndexer, Successful}
import com.blinkbox.books.test.FailHelper
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object Blank
case class E2EContext[+T](client: ElasticClient, indexer: EsIndexer, state: T)

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

  def using(client: ElasticClient, indexer: EsIndexer) = E2EContext(client, indexer, Blank)

  implicit class CommonOps[S](context: E2EContext[S])(implicit ec: ExecutionContext) {
    def flush(idx: String = "_all") = context.client.flush(idx)
    def advance[T](stateFn: S => T): E2EContext[T] = context.copy(state = stateFn(context.state))
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
    def createIndex(indexDef: CreateIndexDefinition): E2EContext[Future[CreateIndexResponse]] =
      context advance (for {
        _   <- context.client execute { delete index "_all" }
        idx <- context.client execute indexDef
        _   <- context.flush()
      } yield idx)
  }

  implicit class WithDefinitionOps(context: E2EContext[Future[CreateIndexResponse]])(implicit ec: ExecutionContext) {
    def index(content: Book*): E2EContext[Future[Iterable[BulkItemResponse]]] =
      context advance { state =>
        (for {
          resp    <- state if (resp.isAcknowledged)
          idxResp <- context.indexer index content
          _       <- context.flush()
        } yield idxResp) recover {
          case _: NoSuchElementException =>
            throw new RuntimeException("Mapping creation has not been acknowledged")
        }
      }

    def indexAndCheck(content: Book*): E2EContext[Future[Iterable[BulkItemResponse]]] = index(content: _*) ensure allSucceded
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
