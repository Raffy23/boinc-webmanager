package at.happywetter.boinc.web.util

import scala.collection.mutable.ListBuffer
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.AppRouter

/**
  * Created by: 
  *
  * @author Raphael
  * @version 21.09.2017
  */
object DashboardMenuBuilder:

  private var rendered: Boolean = false
  var afterRenderHooks: ListBuffer[() => Unit] = ListBuffer.empty

  def renderClients(clients: List[String]): Unit =
    if (rendered) return
    else rendered = true

    DashboardMenu.removeMenuReferences("boinc-client-entry")

    ClientManager.getGroups.foreach(groups => {
      val clientsInGroup = groups.flatMap(_._2).toSeq
      val ungroupedClients = clients.diff(clientsInGroup)

      ungroupedClients.foreach(client =>
        DashboardMenu.addComputer(BoincClientLayout.link(client), client, Some("boinc-client-entry"))
      )

      groups.keys.foreach(groupHeader => {
        DashboardMenu.addGroup(groupHeader, s"group-$groupHeader", Some("boinc-client-entry"))
        groups(groupHeader).foreach(client =>
          if (clients.contains(client))
            DashboardMenu.addComputerToGroup(BoincClientLayout.link(client), client, s"group-$groupHeader")
        )
      })

      AppRouter.router.updatePageLinks()

      afterRenderHooks.foreach(_())
      afterRenderHooks = ListBuffer.empty
    })

  def renderClients(): Unit =
    ClientManager.readClients().map(renderClients)

  def invalidateCache(): Unit =
    rendered = false
    renderClients()
