package at.happywetter.boinc.web.storage

import at.happywetter.boinc.web.storage.IDBCursorRequest.TransactionException
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{ErrorEvent, IDBCursorWithValue, IDBRequest}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
 * Created by: 
 *
 * @author Raphael
 * @version 16.02.2020
 */
object IDBCursorRequest {

  // TODO: 
  class TransactionException(event: Event)
    extends RuntimeException(s"${event.`type`}")

  implicit class IDBRequestConverter(private val request: IDBRequest) extends AnyVal {
    def toCursor[A]: IDBCursorRequest[A] = new IDBCursorRequest[A](request)
  }

}

class IDBCursorRequest[A](val cursor: IDBRequest) {

  def head[B](f: (IDBCursorWithValue, A) => B): Future[Option[B]] = {
    val promise = Promise[Option[B]]()
    var result: Option[B] = None

    cursor.onsuccess = { event =>
      val cursorResult = event.target.asInstanceOf[IDBRequest].result
      if (cursorResult != null) {
        val cursor  = cursorResult.asInstanceOf[IDBCursorWithValue]
        val resultA = cursor.value.asInstanceOf[js.UndefOr[A]]

        resultA.toOption.foreach { resultA =>
          result = Some(f(cursor, resultA))
        }
      }
    }

    cursor.transaction.oncomplete = { _ =>
      promise.success(result)
    }

    cursor.transaction.onerror = { event =>
      // TODO: This changes from org.scalajs.dom.raw.ErrorEvent to org.scalajs.dom.raw.Event ?
      promise.failure(new TransactionException(event.asInstanceOf[ErrorEvent]))
    }

    promise.future
  }

  def foreach(f: (IDBCursorWithValue, A) => Unit): Future[Unit] = {
    val promise = Promise[Unit]()

    cursor.onsuccess = { event =>
      val cursorResult = event.target.asInstanceOf[IDBRequest].result
      if (cursorResult != null) {
        val cursor  = cursorResult.asInstanceOf[IDBCursorWithValue]
        val resultA = cursor.value.asInstanceOf[js.UndefOr[A]]

        resultA.toOption.foreach { resultA =>
          f(cursor, resultA)
        }

        cursor.continue()
      }
    }

    cursor.transaction.oncomplete = { _ =>
      promise.success(())
    }

    cursor.transaction.onerror = { event =>
      promise.failure(new TransactionException(event))
    }

    promise.future
  }

  def map[B](f: A => B): Future[Seq[B]] = {
    val promise = Promise[Seq[B]]()
    var result  = List.empty[B]

    cursor.onsuccess = { event =>
      val cursorResult = event.target.asInstanceOf[IDBRequest].result
      if (cursorResult != null) {
        val cursor  = cursorResult.asInstanceOf[IDBCursorWithValue]
        val resultA = cursor.value.asInstanceOf[js.UndefOr[A]]

        resultA.toOption.foreach { resultA =>
          result = f(resultA) :: result
        }

        cursor.continue()
      }
    }

    cursor.transaction.oncomplete = { _ =>
      promise.success(result)
    }

    cursor.transaction.onerror = { event =>
      promise.failure(new TransactionException(event))
    }

    promise.future
  }

  def fold[B](start: B)(f: (B, IDBCursorWithValue, A) => B): Future[B] = {
    val promise = Promise[B]()
    var result  = start

    cursor.onsuccess = { event =>
      val cursorResult = event.target.asInstanceOf[IDBRequest].result
      if (cursorResult != null) {
        val cursor  = cursorResult.asInstanceOf[IDBCursorWithValue]
        val resultA = cursor.value.asInstanceOf[js.UndefOr[A]]

        resultA.toOption.foreach { resultA =>
          result = f(result, cursor, resultA)
        }

        cursor.continue()
      }
    }

    cursor.transaction.oncomplete = { _ =>
      promise.success(result)
    }

    cursor.transaction.onerror = { event =>
      promise.failure(new TransactionException(event))
    }

    promise.future
  }

}
