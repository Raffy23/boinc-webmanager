package at.happywetter.boinc.web.storage

import org.scalajs.dom
import org.scalajs.dom.raw._

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

  protected def transaction(implicit storeNames: js.Array[String], objStore: String): Future[IDBObjectStore] =
    database.map(r => r.transaction(storeNames, "readwrite").objectStore(objStore))


  protected implicit def IDBRequestToFuture[A](request: IDBRequest): Future[Option[A]] = new Promise[Option[A]]((resolve, reject) => {
    request.onsuccess = (_) => resolve(request.result.asInstanceOf[js.UndefOr[A]].toOption)
    request.onerror = reject
  }).toFuture


  def deleteDatabase(): Future[Boolean] = new Promise[Boolean]((resolve, reject) => {
    val result = dom.window.indexedDB.deleteDatabase("BoincCache")
    result.onerror = reject
    result.onsuccess = (_) => resolve(result.result == js.undefined)
  }).toFuture

}
