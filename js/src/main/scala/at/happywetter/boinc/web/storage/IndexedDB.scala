package at.happywetter.boinc.web.storage

import org.scalajs.dom
import org.scalajs.dom.raw.{IDBDatabase, IDBFactory}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Promise

/**
 * Created by: 
 *
 * @author Raphael
 * @version 05.02.2020
 */
object IndexedDB {

  val DATABASE_NAME    = "BoincClientCache"
  val DATABASE_VERSION = 1

  private[storage] lazy val database: Future[IDBDatabase] = new Promise[IDBDatabase]((resolve, reject) => {
    if (js.isUndefined(dom.window.indexedDB))
      throw new RuntimeException("IndexDB is undefined!")

    val indexedDB = dom.window.indexedDB.asInstanceOf[IDBFactory]
    val db = indexedDB.open(DATABASE_NAME, DATABASE_VERSION)

    db.onerror = reject
    db.onsuccess = (_) => resolve(db.result.asInstanceOf[IDBDatabase])
    db.onupgradeneeded = (_) => {
      /* Create Database */
      val database = db.result.asInstanceOf[IDBDatabase]
      database.createObjectStore(ProjectNameCache.objStore)
      database.createObjectStore(TaskSpecCache.objStore)

      val appSettingsStore = database.createObjectStore(
        AppSettingsStorage.objStore,
        js.Dynamic.literal("keyPath" -> js.Array[String]("client", "wuname"))
      )
      appSettingsStore.createIndex("boinc-client-idx", "client")

      val messagesStore = database.createObjectStore(
        MessageCache.objStore,
        js.Dynamic.literal("keyPath" -> js.Array[String]("boincId", "seqno"))
      )
      messagesStore.createIndex("boincId_idx", "boincId")
    }
  }).toFuture

  def deleteDatabase(): Future[Boolean] = new Promise[Boolean]((resolve, reject) => {
    val result = dom.window.indexedDB.asInstanceOf[IDBFactory].deleteDatabase(IndexedDB.DATABASE_NAME)
    result.onerror = reject
    result.onsuccess = (_) => resolve(js.isUndefined(result.result))
  }).toFuture

}
