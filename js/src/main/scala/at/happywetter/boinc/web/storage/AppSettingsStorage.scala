package at.happywetter.boinc.web.storage

import at.happywetter.boinc.shared.boincrpc.Workunit
import org.scalajs.dom.{IDBCursorWithValue, IDBRequest}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{Date, Promise}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 04.08.2017
  */
object AppSettingsStorage extends DatabaseProvider:

  implicit private[storage] val objStore: String = "workunit_storage"
  implicit private val storeNames: js.Array[String] = js.Array(objStore)

  @js.native
  private trait StorageObject extends js.Object:
    val appName: String = js.native
    val client: String = js.native
    val wuname: String = js.native
    val timestamp: Double = js.native

  def save(boincName: String, wu: Workunit): Future[Unit] =
    transaction(_.put(toJSLiteral(boincName, wu)))

  def get(boincName: String, wuName: String): Future[Option[Workunit]] =
    transactionAsync(f => unpack(f.get(js.Array[String](boincName, wuName))))

  def delete(before: js.Date): Future[Unit] =
    transaction { transaction =>
      val cursor = transaction.openCursor()

      cursor.onsuccess = { event =>
        val cursorResult = event.target.asInstanceOf[IDBRequest[_, IDBCursorWithValue[_]]].result
        if (cursorResult != null)
          val cursor = cursorResult
          val result = cursor.value.asInstanceOf[js.UndefOr[StorageObject]]

          result.toOption.foreach { result =>
            if (result.timestamp < before.getTime())
              cursor.delete()
          }

          cursor.continue()
      }
    }

  private def toJSLiteral(boincName: String, wu: Workunit) =
    js.Dynamic.literal(
      "appName" -> wu.appName,
      "client" -> boincName,
      "wuname" -> wu.name,
      "timestamp" -> new Date().getTime()
    )

  private def toScala(value: js.UndefOr[StorageObject]): Option[Workunit] =
    value.toOption.map(any => Workunit(any.wuname, any.appName, 0d, 0d, 0d, 0d))

  private def unpack(request: IDBRequest[_, _]): Future[Option[Workunit]] =
    new Promise[Option[Workunit]]((resolve, reject) => {
      request.onsuccess = _ =>
        resolve(
          toScala(request.result.asInstanceOf[js.UndefOr[StorageObject]])
        )
      request.onerror = reject
    }).toFuture
