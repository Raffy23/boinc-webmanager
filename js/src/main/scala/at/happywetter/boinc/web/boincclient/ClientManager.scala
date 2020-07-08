package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.webrpc.{AddNewHostRequestBody, BoincProjectMetaData}
import at.happywetter.boinc.web.helper.{FetchHelper, ServerConfig, WebSocketClient}
import org.scalajs.dom
import upickle.default._

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date
import scala.util.Try
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.shared.websocket
import at.happywetter.boinc.web.helper.RichRx._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.07.2017
  */
object ClientManager {

  private val baseURI = "/api/boinc"
  private val cacheTimeout = ServerConfig.config.map(_.hostNameCacheTimeout)

  private val CACHE_CLIENTS = "clientmanager/clients"
  private val CACHE_GROUPS  = "clientmanager/groups"
  private val CACHE_REFRESH_TIME = "clientmanager/lastrefresh"
  private val CACHE_VERSION = "clientmanager/version"

  val clients: mutable.Map[String, BoincClient] = new mutable.HashMap[String, BoincClient]()
  val healthy: mutable.Map[String, Boolean] = new mutable.HashMap[String, Boolean]()

  private var groups: Map[String, List[String]] = _

  init()
  private def init(): Unit = {
    loadFromLocalStorage[List[String]](CACHE_CLIENTS).foreach(_.foreach(c => clients += (c -> new BoincClient(c))))
    loadFromLocalStorage[Map[String, List[String]]](CACHE_GROUPS).foreach(groups = _)

    WebSocketClient.send(websocket.SubscribeToGroupChanges)
    WebSocketClient.listener.append{
      case websocket.HostInformationChanged(newHosts, newGroups) =>
        groups = newGroups
        clients.clear()
        newHosts.foreach(c => clients += (c -> new BoincClient(c)))

      case _ => /* Do nothing, other messages ... */
    }

    val serverVersion = FetchHelper.get[Long]("/api/version")
    serverVersion.foreach { serverVersion =>
      if (loadFromLocalStorage[Long](CACHE_VERSION).getOrElse(0L) != serverVersion) {
        dom.window.localStorage.removeItem(CACHE_REFRESH_TIME)
        readClientsFromServer().foreach { _ =>
          dom.window.localStorage.setItem(CACHE_VERSION, write(serverVersion))
        }
      }
    }

  }

  private def persistClientsIntoStorage(clients: List[String]): Unit = {
    val timestamp = Try(new Date(dom.window.localStorage.getItem(CACHE_REFRESH_TIME)))
    timestamp.fold(
      _ => {
        saveToCache(clients)
        dom.console.log("Clientmanager: Could not read timestamp, persisting current dataset")
      },
      date => {
        val current = new Date()
        if (current.getTime() - date.getTime() > cacheTimeout.now) {
          saveToCache(clients)
          dom.console.log("Clientmanager: Updated Clientlist")
        }
      }
    )
  }


  private def saveToCache(clients: List[String]): Unit = {
    dom.window.localStorage.setItem(CACHE_REFRESH_TIME, new Date().toUTCString())
    dom.window.localStorage.setItem(CACHE_CLIENTS, write(clients))
    dom.window.localStorage.setItem(CACHE_GROUPS, write(groups))
  }

  private def readClientsFromServer(): Future[List[String]] = {
    val timestamp = Try(new Date(dom.window.localStorage.getItem(CACHE_REFRESH_TIME)))

    timestamp.map(date => {
      val current = new Date()

      if (current.getTime() - date.getTime() > cacheTimeout.now) {
        println("Old Cache, reloading groups and clients ...")
        queryGroups.flatMap(data => {
          groups = data
          queryClientsFromServer()
        })
      } else {
        Future { clients.keys.toList }
      }

    }).recover{ case _: Exception => queryClientsFromServer() }
      .get
      .map(_.sorted)
  }


  def readClients(): Future[List[String]] = {
    readClientsFromServer().map(data => {
      data.foreach(c => if(!clients.contains(c)) clients += (c -> new BoincClient(c)))
      persistClientsIntoStorage(data)

      data.sorted(ord = StringLengthAlphaOrdering)
    })
  }

  def getClients: Future[List[BoincClient]] = readClients().map(_.map(new BoincClient(_)))

  def getGroups: Future[Map[String, List[String]]] =
    if (groups == null) readClientsFromServer().map(_ => groups)
    else Future { groups }

  /*
  def bootstrapClients(): Future[Map[String, BoincClient]] = {
    readClientsFromServer().map(clientList => {
      clientList.foreach(c => if(clients.get(c).isEmpty) clients += (c -> new BoincClient(c)))
      persistClientsIntoStorage(clientList)
      clients.toMap
    })
  }
  */

  def addClient(name: String, address: String, port: Int, password: String): Future[Boolean] =
    FetchHelper.post[AddNewHostRequestBody, Boolean](s"$baseURI/$name", AddNewHostRequestBody(address, port, password))

  def removeClient(name: String): Future[Boolean] =
    FetchHelper.delete[Boolean](s"$baseURI/$name")

  def queryClientHealth(): Future[Map[String, Boolean]] =
    FetchHelper.get[Map[String, Boolean]](baseURI + "/health")

  def queryCompleteProjectList(): Future[Map[String,BoincProjectMetaData]] =
    FetchHelper.get[Map[String, BoincProjectMetaData]](baseURI + "/project_list")

  private def queryGroups: Future[Map[String, List[String]]] =
    FetchHelper.get[Map[String, List[String]]]("/api/groups")

  private def queryClientsFromServer(): Future[List[String]] =
    FetchHelper.get[List[String]](baseURI)

  private def queryClientsFromCache(): Future[List[String]] = Future {
    read[List[String]](dom.window.localStorage.getItem(CACHE_CLIENTS))
  }

  private def loadFromLocalStorage[T](name: String)(implicit r: Reader[T]): Option[T] = {
    val cachedClients = dom.window.localStorage.getItem(name)
    if (cachedClients != null) Some(read[T](cachedClients))
    else None
  }

}
