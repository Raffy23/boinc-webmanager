package at.happywetter.boinc.web.boincclient



import at.happywetter.boinc.shared.{BoincProjectMetaData, BoincRPC, WorkunitRequestBody}
import at.happywetter.boinc.web.helper.FetchHelper
import org.scalajs.dom
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}
import prickle.{Pickle, Unpickle}

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
  private val cacheTimeout = 30 * 60* 1000

  val clients: mutable.Map[String, BoincClient] = new mutable.HashMap[String, BoincClient]()
  Unpickle[List[String]]
    .fromString(dom.window.localStorage.getItem("clientmanager/clients"))
    .getOrElse(List())
    .foreach(c => clients += (c -> new BoincClient(c)))

  private def persistClientsIntoStorage(clients: List[String]): Unit = {
    import prickle._

    val timestamp = Try(new Date(dom.window.localStorage.getItem("clientmanager/lastrefresh")))
    timestamp.fold(
      ex => {
        dom.window.localStorage.setItem("clientmanager/lastrefresh", new Date().toUTCString())
        dom.window.localStorage.setItem("clientmanager/clients", Pickle.intoString(clients))
        dom.console.log("Clientmanager: Could not read timestamp, persist current Clientlist")
      },
      date => {
        val current = new Date()
        if (current.getTime()-date.getTime() > cacheTimeout) {
          dom.window.localStorage.setItem("clientmanager/lastrefresh", new Date().toUTCString())
          dom.window.localStorage.setItem("clientmanager/clients", Pickle.intoString(clients))
          dom.console.log("Clientmanager: Updated Clientlist")
        }
      }
    )
  }

  def readClients(): Future[List[String]] = {
    import prickle._

    val timestamp = Try(new Date(dom.window.localStorage.getItem("clientmanager/lastrefresh")))

    timestamp.map(date => {
      val current = new Date()
      if (current.getTime()-date.getTime() > cacheTimeout) {
        Fetch.fetch(baseURI, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
          .toFuture
          .flatMap(response => response.text().toFuture)
          .map(data => Unpickle[List[String]].fromString(json = data).get)
      } else {
        Future {
          val clients = Unpickle[List[String]].fromString(dom.window.localStorage.getItem("clientmanager/clients"))
          //TODO: Make fallback if cache is corrupted
          clients.get
        }
      }
    }).get
  }

  def bootstrapClients(): Future[Map[String, BoincClient]] = {
    println("bootstrap")
    readClients().map(clientList => {
      clientList.foreach(c => if(clients.get(c).isEmpty) clients += (c -> new BoincClient(c)))
      println(clientList)
      persistClientsIntoStorage(clientList)
      clients.toMap
    })
  }

  def queryCompleteProjectList(): Future[Map[String,BoincProjectMetaData]] = {
    Fetch
      .fetch(
        baseURI + "/project_list",
        RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
      )
      .toFuture
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Map[String,BoincProjectMetaData]].fromString(json = data).get)
  }
}
