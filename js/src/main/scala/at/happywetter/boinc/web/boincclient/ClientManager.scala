package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.webrpc.BoincProjectMetaData
import at.happywetter.boinc.web.helper.{FetchHelper, ServerConfig}
import org.scalajs.dom
import upickle.default._

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date
import scala.util.Try
import at.happywetter.boinc.shared.parser._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.07.2017
  */
object ClientManager {

  private val baseURI = "/api/boinc"
  private def cacheTimeout = ServerConfig.get.map(_.hostNameCacheTimeout)

  val clients: mutable.Map[String, BoincClient] = new mutable.HashMap[String, BoincClient]()
  val healthy: mutable.Map[String, Boolean] = new mutable.HashMap[String, Boolean]()

  private var groups: Map[String, List[String]] = _

  init()
  private def init(): Unit = {
    val cachedClients = dom.window.localStorage.getItem("clientmanager/clients")
    if (cachedClients != null)
      read[List[String]](cachedClients).foreach(c => clients += (c -> new BoincClient(c)))

  }

  private def persistClientsIntoStorage(clients: List[String]): Unit = {
    cacheTimeout.foreach(cacheTimeout => {
      val timestamp = Try(new Date(dom.window.localStorage.getItem("clientmanager/lastrefresh")))
      timestamp.fold(
        ex => {
          dom.window.localStorage.setItem("clientmanager/lastrefresh", new Date().toUTCString())
          dom.window.localStorage.setItem("clientmanager/clients", write(clients))
          dom.console.log("Clientmanager: Could not read timestamp, persist current Clientlist")
        },
        date => {
          val current = new Date()
          if (current.getTime() - date.getTime() > cacheTimeout) {
            dom.window.localStorage.setItem("clientmanager/lastrefresh", new Date().toUTCString())
            dom.window.localStorage.setItem("clientmanager/clients", write(clients))
            dom.console.log("Clientmanager: Updated Clientlist")
          }
        }
      )
    })
  }

  private def readClientsFromServer(): Future[List[String]] = {
    val timestamp = Try(new Date(dom.window.localStorage.getItem("clientmanager/lastrefresh")))

    cacheTimeout.flatMap(cacheTimeout => {
      timestamp.map(date => {
        val current = new Date()

        if (current.getTime() - date.getTime() > cacheTimeout) {
          queryGroups.flatMap(data => {
            groups = data
            queryClientsFromServer()
          })
        } else {
          queryGroups.flatMap(data => {
            groups = data
            queryClientsFromCache()
          })
        }

      }).recover{ case _: Exception => queryClientsFromServer() }.get
    }).map(_.sorted)
  }

  def readClients(): Future[List[String]] = {
    readClientsFromServer().map(data => {
      data.foreach(c => if(clients.get(c).isEmpty) clients += (c -> new BoincClient(c)))
      persistClientsIntoStorage(data)
      data
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

  def queryClientHealth(): Future[Map[String, Boolean]] =
    FetchHelper.get[Map[String, Boolean]](baseURI + "/health")

  def queryCompleteProjectList(): Future[Map[String,BoincProjectMetaData]] =
    FetchHelper.get[Map[String, BoincProjectMetaData]](baseURI + "/project_list")

  private def queryGroups: Future[Map[String, List[String]]] =
    FetchHelper.get[Map[String, List[String]]]("/api/groups")

  private def queryClientsFromServer(): Future[List[String]] =
    FetchHelper.get[List[String]](baseURI)

  private def queryClientsFromCache(): Future[List[String]] = Future {
    read[List[String]](dom.window.localStorage.getItem("clientmanager/clients"))
  }

}
