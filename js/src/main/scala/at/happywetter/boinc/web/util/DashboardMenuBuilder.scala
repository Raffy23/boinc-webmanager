package at.happywetter.boinc.web.util

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.{BoincClientLocation, DashboardLocation}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 21.09.2017
  */
object DashboardMenuBuilder {
  private var rendered: Boolean = false

  def renderClients(clients: List[String]): Unit = {
    if (rendered) return
    else rendered = true

    DashboardMenu.removeMenuReferences("boinc-client-entry")

    ClientManager.getGroups.foreach( groups => {
      val clientsInGroup   = groups.flatMap(_._2).toSeq
      val ungroupedClients = clients.diff(clientsInGroup)

      ungroupedClients.foreach(client =>
        DashboardMenu.addMenu(s"${BoincClientLocation.link}/$client",client, Some("boinc-client-entry"))
      )

      groups.keys.foreach( groupHeader => {
        DashboardMenu.addSubMenu(groupHeader, s"group-$groupHeader", Some("boinc-client-entry"))
        groups(groupHeader).foreach(client =>
          if (clients.contains(client))
            DashboardMenu.addSubMenuItem(s"${BoincClientLocation.link}/$client", client, s"group-$groupHeader")
        )
      })

      AppRouter.router.updatePageLinks()
    })
  }

  def renderClients(): Unit =
    ClientManager.readClients().map(renderClients)

}
