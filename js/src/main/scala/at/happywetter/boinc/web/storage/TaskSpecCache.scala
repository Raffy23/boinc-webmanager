package at.happywetter.boinc.web.storage

import scala.concurrent.Future
import scala.scalajs.js
import at.happywetter.boinc.shared.App
import org.scalajs.dom
import org.scalajs.dom.raw.IDBRequest

import scala.scalajs.js.{Date, Promise}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
object TaskSpecCache extends DatabaseProvider {

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  private implicit val objStore: String = "task_cache"
  private implicit val storeNames = js.Array("task_cache")

  import prickle._

  def save(boincName: String, appName: String, app: App): Future[Unit] =
    transaction.map(f => f.add(Pickle.intoString(app), boincName+"/"+appName))

  def get(boincName: String, appName: String): Future[Option[App]] =
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
    request.onsuccess = (_) => resolve(request.result.asInstanceOf[js.UndefOr[String]].toOption.flatMap(a => Unpickle[App].fromString(a).toOption))
    request.onerror = reject
  }).toFuture
}
