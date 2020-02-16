package at.happywetter.boinc.web.storage

import at.happywetter.boinc.shared.boincrpc.Message
import org.scalajs.dom.raw.{IDBCursorWithValue, IDBKeyRange, IDBRequest}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

import at.happywetter.boinc.shared.parser.messageParser
import upickle.default._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 05.02.2020
 */
object MessageCache extends DatabaseProvider {

  private[storage] implicit val objStore: String = "messages"
  private implicit val storeNames: js.Array[String] = js.Array(objStore)

  @js.native
  private trait StorageObject extends js.Object {
    val boincId: String = js.native
    val seqno: Int = js.native
    val msg: String = js.native
  }

  def save(boincName: String, messages: List[Message]): Future[Unit] =
    transaction { transaction =>
      messages.foreach { msg =>
        transaction.put(
          js.Dynamic.literal(
            "boincId" -> boincName,
            "seqno" -> msg.seqno.toInt,
            "msg" -> write(msg)
          )
        )
      }
    }

  def get(boincName: String, lower: Int, upper: Int): Future[List[Message]] =
    transactionAsync { transaction =>

      val lowerBound = js.Array[js.Any](boincName, lower)
      val upperBound = js.Array[js.Any](boincName, upper)
      val keyRange = IDBKeyRange.bound(lowerBound, upperBound)

      val cursor   = transaction.openCursor(keyRange)
      val promise  = Promise[List[Message]]
      var tmpQuery = List.empty[Message]

      cursor.onsuccess = { event =>
        val cursorResult = event.target.asInstanceOf[IDBRequest].result
        if (cursorResult != null) {
          val cursor = cursorResult.asInstanceOf[IDBCursorWithValue]
          val result = cursor.value.asInstanceOf[js.UndefOr[StorageObject]]

          result.toOption.foreach { result =>
            tmpQuery = read[Message](result.msg) :: tmpQuery
          }

          cursor.continue()
        }
      }

      cursor.transaction.oncomplete = { _ =>
        promise.success(tmpQuery)
      }

      cursor.transaction.onerror = { event =>
        promise.failure(new RuntimeException(s"${event.message} in ${event.filename}:${event.lineno}"))
      }

      promise.future
    }

  def get(name: String): Future[List[Message]] = ???

  def delete(name: String): Future[Int] =
    transactionAsync { tx =>
      val keyRange = IDBKeyRange.only(name)
      val cursor   = tx.index("boincId_idx").openCursor(keyRange)
      val promise  = Promise[Int]
      var count    = 0

      cursor.onsuccess = { event =>
        val cursorResult = event.target.asInstanceOf[IDBRequest].result
        if (cursorResult != null) {
          val cursor = cursorResult.asInstanceOf[IDBCursorWithValue]
          count += 1

          cursor.delete()
          cursor.continue()
        }
      }

      cursor.transaction.oncomplete = { _ =>
        promise.success(count)
      }

      cursor.transaction.onerror = { event =>
        promise.failure(new RuntimeException(s"${event.message} in ${event.filename}:${event.lineno}"))
      }

      promise.future
    }

  def getLastSeqNo(name: String): Future[Int] =
    transactionAsync { tx =>
      val keyRange = IDBKeyRange.only(name)
      val cursor   = tx.index("boincId_idx").openCursor(keyRange, "prev")
      val promise  = Promise[Int]

      cursor.onsuccess = { event =>
        val cursorResult = event.target.asInstanceOf[IDBRequest].result
        if (cursorResult != null) {
          val cursor = cursorResult.asInstanceOf[IDBCursorWithValue]
          val result = cursor.value.asInstanceOf[js.UndefOr[StorageObject]]

          result.toOption.foreach { result =>
            promise.success(result.seqno)
          }
        }
      }

      cursor.transaction.onerror = { event =>
        promise.failure(new RuntimeException(s"${event.message} in ${event.filename}:${event.lineno}"))
      }

      promise.future
    }

}
