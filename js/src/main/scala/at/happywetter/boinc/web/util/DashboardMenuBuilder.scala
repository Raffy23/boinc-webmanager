package at.happywetter.boinc.web.util

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.DashboardLocation
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 21.09.2017
  */
object DashboardMenuBuilder {


  def renderClients(clients: List[String]): Unit = {
    DashboardMenu.removeMenuReferences("boinc-client-entry")

    ClientManager.getGroups.foreach( groups => {
      val ungroupedClients = clients.diff(groups.flatMap(_._2).toSeq)
      ungroupedClients.foreach(client =>
        DashboardMenu.addMenu(s"${AppRouter.href(DashboardLocation)}/$client",client, Some("boinc-client-entry"))
      )

      groups.keys.foreach( groupHeader => {
        DashboardMenu.addSubMenu(groupHeader, s"group-$groupHeader")
        groups(groupHeader).foreach(client =>
          DashboardMenu.addSubMenuItem(s"${AppRouter.href(DashboardLocation)}/$client", client, s"group-$groupHeader", Some("boinc-client-entry"))
        )
      })

      AppRouter.router.updatePageLinks()
    })
  }

  def renderClients(): Unit =
    ClientManager.readClients().map(renderClients)

}
