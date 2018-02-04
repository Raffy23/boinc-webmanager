package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.BoincProjectMetaData
import at.happywetter.boinc.web.boincclient.ClientManager.queryGroups
import at.happywetter.boinc.web.helper.ResponseHelper._
import at.happywetter.boinc.web.helper.{FetchHelper, ServerConfig}
import org.scalajs.dom
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date
import scala.util.Try

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

  private var groups: Map[String, List[String]] = null
  decode[List[String]](dom.window.localStorage.getItem("clientmanager/clients"))
    .getOrElse(List())
    .foreach(c => clients += (c -> new BoincClient(c)))

  private def persistClientsIntoStorage(clients: List[String]): Unit = {
    cacheTimeout.foreach(cacheTimeout => {
      val timestamp = Try(new Date(dom.window.localStorage.getItem("clientmanager/lastrefresh")))
      timestamp.fold(
        ex => {
          dom.window.localStorage.setItem("clientmanager/lastrefresh", new Date().toUTCString())
          dom.window.localStorage.setItem("clientmanager/clients", clients.asJson.noSpaces)
          dom.console.log("Clientmanager: Could not read timestamp, persist current Clientlist")
        },
        date => {
          val current = new Date()
          if (current.getTime() - date.getTime() > cacheTimeout) {
            dom.window.localStorage.setItem("clientmanager/lastrefresh", new Date().toUTCString())
            dom.window.localStorage.setItem("clientmanager/clients", clients.asJson.noSpaces)
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
    }).map(_.sortWith(_ < _))
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

  def queryClientHealth(): Future[Map[String, Boolean]] =  {
    Fetch
      .fetch(
        baseURI + "/health",
        RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
      )
      .mapData(data => decode[Map[String, Boolean]](data).toOption.get)
  }

  def queryCompleteProjectList(): Future[Map[String,BoincProjectMetaData]] = {
    Fetch
      .fetch(
        baseURI + "/project_list",
        RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
      )
      .mapData(data => decode[Map[String,BoincProjectMetaData]](data).toOption.get)
  }


  private def queryGroups: Future[Map[String, List[String]]] =
    Fetch
      .fetch("/api/groups", RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[Map[String, List[String]]](data).toOption.get)

  private def queryClientsFromServer(): Future[List[String]] =
    Fetch
      .fetch(baseURI, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[List[String]](data).toOption.get)

  private def queryClientsFromCache(): Future[List[String]] = Future {
    val clients = decode[List[String]](dom.window.localStorage.getItem("clientmanager/clients")).toOption
    //TODO: Make fallback if cache is corrupted
    clients.get
  }
}
