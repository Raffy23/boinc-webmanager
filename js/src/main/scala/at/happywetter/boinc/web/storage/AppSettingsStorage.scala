package at.happywetter.boinc.web.storage

import at.happywetter.boinc.shared.boincrpc.Workunit
import at.happywetter.boinc.web.helper.CompatibilityTester
import org.scalajs.dom.raw.IDBRequest

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Promise

/**
  * Created by: 
  *
  * @author Raphael
  * @version 04.08.2017
  */
object AppSettingsStorage extends DatabaseProvider {
  private implicit val objStore: String = "workunit_storage"
  private implicit val storeNames = js.Array("workunit_storage")

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  @js.native
  private trait StorageObject extends js.Object {
    val appName: String = js.native
    val client: String = js.native
    val wuname: String = js.native
  }

  def save(boincName: String, wu: Workunit): Future[Unit] =
    if (CompatibilityTester.isFirefox) firefoxTransaction(_.put(toJSLiteral(boincName, wu)))
    else transaction.map(f => f.put(toJSLiteral(boincName, wu)))

  def get(boincName: String, wuName: String): Future[Option[Workunit]] = {
    val key = new js.Array[String]
    key.push(boincName)
    key.push(wuName)

    if (CompatibilityTester.isFirefox) firefoxTransactionAsync(f => unpack(f.get(key)))
    else transaction.flatMap(f => unpack(f.get(key)))
  }

  private def toJSLiteral(boincName: String, wu: Workunit) =
    js.Dynamic.literal("appName" -> wu.appName,"client"->boincName, "wuname"->wu.name)

  private def toScala(value: js.UndefOr[StorageObject]): Option[Workunit] = {
    value.toOption.map(any => Workunit(any.wuname,any.appName,0D,0D,0D,0D))
  }

  private def unpack(request: IDBRequest): Future[Option[Workunit]] =
    new Promise[Option[Workunit]]((resolve, reject) => {
      request.onsuccess = (_) => resolve(toScala(request.result.asInstanceOf[js.UndefOr[StorageObject]]))
      request.onerror = reject
    }).toFuture
}
