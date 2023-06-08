package at.happywetter.boinc.web.storage

import at.happywetter.boinc.shared.boincrpc.Message
import at.happywetter.boinc.shared.parser.messageParser
import at.happywetter.boinc.web.storage.IDBCursorRequest._
import org.scalajs.dom
import org.scalajs.dom.{IDBCursorDirection, IDBKeyRange}
import upickle.default.*

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Created by: 
 *
 * @author Raphael
 * @version 05.02.2020
 */
object MessageCache extends DatabaseProvider:

  implicit private[storage] val objStore: String = "messages"

  private[storage] val ID_INDEX = "boincId_idx"

  implicit private val storeNames: js.Array[String] = js.Array(objStore)

  @js.native
  private trait StorageObject extends js.Object:
    val boincId: String = js.native
    val seqno: Int = js.native
    val msg: String = js.native

  def save(boincName: String, messages: List[Message]): Future[Unit] =
    transaction { transaction =>
      dom.console.log(s"[MessageCache] Save ${messages.size} messages for $boincName")

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

  def get(boincName: String, lower: Int, upper: Int): Future[Seq[Message]] =
    transactionAsync { transaction =>

      val lowerBound = js.Array[js.Any](boincName, lower)
      val upperBound = js.Array[js.Any](boincName, upper)
      val keyRange = IDBKeyRange.bound(lowerBound, upperBound)

      transaction
        .openCursor(keyRange)
        .toCursor[StorageObject]
        .map(storageObject => read[Message](storageObject.msg))

    }

  def get(name: String): Future[Seq[Message]] =
    transactionAsync:
      _.index(ID_INDEX)
        .openCursor(IDBKeyRange.only(name))
        .toCursor[StorageObject]
        .map(storageObject => read[Message](storageObject.msg))
        .map(_.reverse)

  def delete(name: String): Future[Int] =
    transactionAsync:
      _.index(ID_INDEX)
        .openCursor(IDBKeyRange.only(name))
        .toCursor[Any]
        .fold(0) { case (count, cursor, _) =>
          cursor.delete()
          count + 1
        }

  def getLastSeqNo(name: String): Future[Int] =
    transactionAsync:
      _.index(ID_INDEX)
        .openCursor(IDBKeyRange.only(name), direction = IDBCursorDirection.prev)
        .toCursor[StorageObject]
        .head { case (_, storageObject) =>
          storageObject.seqno
        }
        .map(_.getOrElse(0))
