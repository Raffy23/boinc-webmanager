package at.happywetter.boinc.web.storage

import at.happywetter.boinc.web.util.CompatibilityTester
import org.scalajs.dom
import org.scalajs.dom.raw._

import scala.language.implicitConversions
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
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

  protected lazy val database: Future[IDBDatabase] = IndexedDB.database
  private val MODE_RW = "readwrite"

  protected def transactionAsync[A](f: IDBObjectStore => Future[A])(implicit storeNames: js.Array[String], objStore: String): Future[A] = {
    if (CompatibilityTester.isFirefox) firefoxTransactionAsync(f)
    else                               normalTransaction.flatMap(f)
  }

  protected def transaction[A](f: IDBObjectStore => A)(implicit storeNames: js.Array[String], objStore: String): Future[A] = {
    if (CompatibilityTester.isFirefox) firefoxTransaction(f)
    else                               normalTransaction.map(f)
  }

  // Bug: Firefox *hates* Promises so this does not work in Firefox
  private def normalTransaction(implicit storeNames: js.Array[String], objStore: String): Future[IDBObjectStore] =
    database.map(r => r.transaction(storeNames, MODE_RW).objectStore(objStore))

  private def firefoxTransaction[A](firefoxCallback: IDBObjectStore => A)(implicit storeNames: js.Array[String], objStore: String): Future[A] =
    database.map(r => {
      val ffTransaction = r.transaction(storeNames, MODE_RW)
      val objStorage   = ffTransaction.objectStore(objStore)

      firefoxCallback(objStorage)
    })

  private def firefoxTransactionAsync[A](firefoxCallback: IDBObjectStore => Future[A])(implicit storeNames: js.Array[String], objStore: String): Future[A] =
    database.flatMap(r => {
      val ffTransaction = r.transaction(storeNames, MODE_RW)
      val objStorage   = ffTransaction.objectStore(objStore)

      firefoxCallback(objStorage)
    })

  protected implicit def IDBRequestToFuture[A](request: IDBRequest): Future[Option[A]] = new Promise[Option[A]]((resolve, reject) => {
    request.onsuccess = (_) => resolve(request.result.asInstanceOf[js.UndefOr[A]].toOption)
    request.onerror = reject
  }).toFuture

  implicit class IDBRequestStringFuture(request: IDBRequest) {
    def getData: Future[Option[String]] = IDBRequestToFuture[String](request)
  }

}
