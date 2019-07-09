package at.happywetter.boinc.web.storage

import scala.concurrent.Future
import scala.scalajs.js
import at.happywetter.boinc.shared.boincrpc.App
import at.happywetter.boinc.web.helper.CompatibilityTester
import org.scalajs.dom
import org.scalajs.dom.raw.IDBRequest

import scala.scalajs.js.{Date, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import upickle.default._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
object TaskSpecCache extends DatabaseProvider {
  import at.happywetter.boinc.shared.parser._

  private implicit val objStore: String = "task_cache"
  private implicit val storeNames = js.Array("task_cache")

  def save(boincName: String, appName: String, app: App): Future[Unit] =
    if (CompatibilityTester.isFirefox)
      firefoxTransaction(_.add(write(app), boincName+"/"+appName))
    else
      transaction.map(f => f.add(write(app), boincName+"/"+appName))

  def get(boincName: String, appName: String): Future[Option[App]] =
    if (CompatibilityTester.isFirefox)
      firefoxTransactionAsync(f => unpack(f.get(boincName + "/" + appName)))
    else
      transaction.flatMap(f => unpack(f.get(boincName + "/" + appName)))

  def updateCacheTimeStamp(boincName: String): Unit = {
    dom.window.localStorage.setItem("TaskSpecCache/"+boincName, new Date().toJSON())
  }

  def getCacheTimeStamp(boincName: String): Date = {
    val data = dom.window.localStorage.getItem("TaskSpecCache/"+boincName)

    if (data != null) new Date(dom.window.localStorage.getItem("TaskSpecCache/"+boincName))
    else new Date(0D)

  }

  def isCacheValid(boincName: String): Boolean =
    getCacheTimeStamp(boincName).getTime() * 1000 * 60 * 60 > new Date().getTime()

  private def unpack(request: IDBRequest): Future[Option[App]] =
    new Promise[Option[App]]((resolve, reject) => {
    request.onsuccess = _ => resolve(request.result.asInstanceOf[js.UndefOr[String]].toOption.map(a => read[App](a)))
    request.onerror   = reject
  }).toFuture
}
