package at.happywetter.boinc.web.storage

import org.scalajs.dom
import org.scalajs.dom.IDBRequest
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.{Date, Promise}

import at.happywetter.boinc.shared.boincrpc.App
import at.happywetter.boinc.web.util.CompatibilityTester

import upickle.default._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
object TaskSpecCache extends DatabaseProvider:
  import at.happywetter.boinc.shared.parser._

  implicit private[storage] val objStore: String = "task_cache"
  implicit private val storeNames: js.Array[String] = js.Array(objStore)

  def save(boincName: String, appName: String, app: App): Future[Unit] =
    transaction(f => f.put(write(app), boincName + "/" + appName))

  def get(boincName: String, appName: String): Future[Option[App]] =
    transactionAsync(f => unpack(f.get(boincName + "/" + appName)))

  def updateCacheTimeStamp(boincName: String): Unit =
    dom.window.localStorage.setItem("TaskSpecCache/" + boincName, new Date().toJSON())

  def getCacheTimeStamp(boincName: String): Date =
    val data = dom.window.localStorage.getItem("TaskSpecCache/" + boincName)

    if (data != null) new Date(dom.window.localStorage.getItem("TaskSpecCache/" + boincName))
    else new Date(0d)

  def isCacheValid(boincName: String): Boolean =
    getCacheTimeStamp(boincName).getTime() * 1000 * 60 * 60 > new Date().getTime()

  private def unpack(request: IDBRequest[_, _]): Future[Option[App]] =
    new Promise[Option[App]]((resolve, reject) => {
      request.onsuccess = _ => resolve(request.result.asInstanceOf[js.UndefOr[String]].toOption.map(a => read[App](a)))
      request.onerror = reject
    }).toFuture
