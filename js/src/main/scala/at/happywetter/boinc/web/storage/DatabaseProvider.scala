package at.happywetter.boinc.web.storage

import org.scalajs.dom
import org.scalajs.dom.raw.{IDBTransaction, _}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Promise

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.08.2017
  */
trait DatabaseProvider {

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  import scala.language.implicitConversions

  protected lazy val database: Future[IDBDatabase] = new Promise[IDBDatabase]((resolve, reject) => {
    val db = dom.window.indexedDB.open("BoincCache", 1)
    db.onerror = reject
    db.onsuccess = (_) => resolve(db.result.asInstanceOf[IDBDatabase])
    db.onupgradeneeded = (_) => {
      /* Create Database */
      val database = db.result.asInstanceOf[IDBDatabase]
      database.createObjectStore("project_name_cache")
      database.createObjectStore("task_cache")

      val keys = new js.Array[String]
      keys.push("client")
      keys.push("wuname")

      val store = database.createObjectStore("workunit_storage", js.Dynamic.literal("keyPath" -> keys))
      //store.createIndex("default", "client,wuname")
      store.createIndex("boinc-client-idx", "client")
    }
  }).toFuture

  // Bug: Firefox *hates* Promises so this does not work in FireFox
  protected def transaction(implicit storeNames: js.Array[String], objStore: String): Future[IDBObjectStore] =
    database.map(r => r.transaction(storeNames, "readwrite").objectStore(objStore))

  protected def firefoxTransaction[A](firefoxCallback: (IDBObjectStore) => A)(implicit storeNames: js.Array[String], objStore: String): Future[A] =
    database.map(r => {
      val ffTransaction = r.transaction(storeNames, "readwrite")
      val objStorage   = ffTransaction.objectStore(objStore)

      firefoxCallback(objStorage)
    })

  protected def firefoxTransactionAsync[A](firefoxCallback: (IDBObjectStore) => Future[A])(implicit storeNames: js.Array[String], objStore: String): Future[A] =
    database.flatMap(r => {
      val ffTransaction = r.transaction(storeNames, "readwrite")
      val objStorage   = ffTransaction.objectStore(objStore)

      firefoxCallback(objStorage)
    })

  protected implicit def IDBRequestToFuture[A](request: IDBRequest): Future[Option[A]] = new Promise[Option[A]]((resolve, reject) => {
    request.onsuccess = (_) => resolve(request.result.asInstanceOf[js.UndefOr[A]].toOption)
    request.onerror = reject
  }).toFuture


  def deleteDatabase(): Future[Boolean] = new Promise[Boolean]((resolve, reject) => {
    val result = dom.window.indexedDB.deleteDatabase("BoincCache")
    result.onerror = reject
    result.onsuccess = (_) => resolve(result.result.asInstanceOf[js.UndefOr[_]] == js.undefined)
  }).toFuture

  implicit class IDBRequestStringFuture(request: IDBRequest) {

    def getData: Future[Option[String]] = IDBRequestToFuture[String](request)

  }

}
