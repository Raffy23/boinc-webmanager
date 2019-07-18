package at.happywetter.boinc.web.pages.swarm

import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.Project
import at.happywetter.boinc.shared.boincrpc.BoincRPC
import at.happywetter.boinc.shared.webrpc.ServerStatus
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.helper.FetchHelper
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.boinc.{BoincClientLayout, BoincProjectLayout}
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.pages.component.dialog._
import at.happywetter.boinc.web.pages.swarm.ProjectSwarmPage.Style
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.Elem
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet
import at.happywetter.boinc.web.hacks.Implicits.RichWindow

import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 14.09.2017
  */
object ProjectSwarmPage {
  object Style extends StyleSheet.Inline {
    import dsl._

    import scala.language.postfixOps

    val masterCheckbox = BoincSwarmPage.Style.masterCheckbox
    val checkbox = BoincSwarmPage.Style.checkbox
    val center = BoincSwarmPage.Style.center
    val button = BoincSwarmPage.Style.button
    val link = BoincProjectLayout.Style.link

    val top_nav_action = style(
      color(c"#333"),
      textDecoration := "none",
      fontSize(28 px),
      paddingLeft(8 px)
    )

    val last_row_small = style(
      width(1.5 em)
    )

    val floatingMenu = style(
      position.absolute,
      top(80 px),
      right(20 px),
    )
  }
}

class ProjectSwarmPage extends SwarmPageLayout {
  override val path: String = "projects"

  private val dataset: Var[Map[String, List[(BoincClient, Project)]]] = Var(Map.empty)
  private case class Account(userName: String, teamName: String, credits: Double)

  override val header: String = "project_header".localize

  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    NProgress.start()

    import at.happywetter.boinc.shared.parser._
    ClientManager.getClients.foreach(clients => {
      Future.sequence(
        clients
          .map(client =>
            client.getProjects
              .map(_.map(project => {
                (client, project)
              }))
              .recover { case _: Exception => List() }
          )
      ).map(_.flatten.groupBy(_._2.url))
       .map(projects => dataset := projects)
       .foreach(_ => NProgress.done(true))
    })
  }

  override def renderChildView: Elem = {
    <div id="swarm-project-content">
      <div style="position:absolute;top:80px;right:20px">
        {
          Seq(
            new Tooltip(
              Var("project_swarm_play".localize),
              <a class={Style.top_nav_action.htmlClass} href="#apply-to_all-project"
                 onclick={jsPlayAllSelectedAction(BoincRPC.ProjectAction.Resume)}>
                <i class="fas fa-play-circle"></i>
              </a>,
              textOrientation = Tooltip.Style.topText
            ).toXML,
            new Tooltip(
              Var("project_swarm_pause".localize),
              <a class={Style.top_nav_action.htmlClass} href="#apply-to_all-project"
                 onclick={jsPlayAllSelectedAction(BoincRPC.ProjectAction.Suspend)}>
                <i class="fas fa-pause-circle"></i>
              </a>,
              textOrientation = Tooltip.Style.topText
            ).toXML,
            new Tooltip(
              Var("project_swarm_refresh".localize),
              <a class={Style.top_nav_action.htmlClass} href="#apply-to_all-project"
                 onclick={jsPlayAllSelectedAction(BoincRPC.ProjectAction.Update)}>
                <i class="fas fa-sync"></i>
              </a>,
              textOrientation = Tooltip.Style.topText
            ).toXML,
            new Tooltip(
              Var("project_swarm_trash".localize),
              <a class={Style.top_nav_action.htmlClass} href="#apply-to_all-project"
                 onclick={jsPlayAllSelectedAction(BoincRPC.ProjectAction.Remove)}>
                <i class="fas fa-trash"></i>
              </a>,
              textOrientation = Tooltip.Style.topText
            ).toXML,
            new Tooltip(
              Var("project_new_tooltip".localize),
              <a class={Style.top_nav_action.htmlClass} href="#add-project"
                 onclick={jsAddNewProjectAction}>
                <i class="fa fa-plus-square"></i>
              </a>,
              textOrientation = Tooltip.Style.topText
            ).toXML
          )
        }
      </div>
      <table class={TableTheme.table.htmlClass} id="swarm-project-data-table">
        <thead>
          <tr class={BoincClientLayout.Style.in_text_icon.htmlClass}>
            <th>
              <a class={Style.masterCheckbox.htmlClass} href="#" onclick={jsSelectAllListener}>
                <i class="far fa-check-square"></i>
                {"table_project".localize}
              </a>
            </th>
            <th>{"table_hosts".localize}</th>
            <th>{"table_account".localize}</th>
            <th>{"table_team".localize}</th>
            <th>{"table_credits".localize}</th>
            <th class={Style.last_row_small.htmlClass}></th>
          </tr>
        </thead>
        <tbody>
          {
            dataset.map(projects => {
              projects.toList.sortBy(entry => entry._2.headOption.map(_._2.name).getOrElse(entry._1)).map { case (url, data) =>
                val project = data.head._2
                val accounts = data.map { case (_, project) => Account(project.userName, project.teamName, project.userAvgCredit) }
                val creditsRange = (accounts.map(_.credits).min, accounts.map(_.credits).max)

                <tr>
                  <td style="max-width:100px">
                    <input type="checkbox" value={url}></input>
                    <a class={Style.link.htmlClass} href={project.url} onclick={AppRouter.openExternal}>{updateCache(project)}</a>
                  </td>
                  <td style="text-align:center">{data.size}</td>
                  <td>{accounts.map(t => t.userName).distinct.flatMap(t => List(t.toXML, <br/>))}</td>
                  <td>{accounts.map(t => t.teamName).distinct.flatMap(t => List(t.toXML, <br/>))}</td>
                  <td>{creditsRange._1 + " - " + creditsRange._2}</td>
                  <td>
                    {
                      new Tooltip(
                        Var("project_properties".localize),
                        <a href="#project-properties" onclick={jsShowProjectProperties(url, data)}>
                          <i class="fa fa-info-circle"></i>
                        </a>,
                        textOrientation = Tooltip.Style.leftText
                      ).toXML
                    }
                  </td>
                </tr>
              }
            })
          }
        </tbody>
      </table>
    </div>
  }

  private lazy val jsAddNewProjectAction: (Event) => Unit = (event) => {
    event.preventDefault()

    //TODO: Use some cache ...
    ClientManager.queryCompleteProjectList().foreach(data => {
      new ProjectAddDialog(data, (url, username, password, name) => {
        NProgress.start()

        ClientManager
          .getClients
          .map(_.map(
            _.attachProject(url, username, password, name).recover { case _: Exception => false })
          ).flatMap(
          Future
            .sequence(_)
            .map(_.count(_.unary_!))
            .map(failures => {
              if (failures > 0) {
                new OkDialog("dialog_error_header".localize, List("project_new_error_msg".localize), (_) => {
                  dom.document.getElementById("pad-username").asInstanceOf[HTMLElement].focus()
                }).renderToBody().show()
              }

              NProgress.done(true)
              failures == 0
            })
        )

      }).renderToBody().show()
    })
  }

  private def jsShowProjectProperties(project: String, data: List[(BoincClient, Project)]): (Event) => Unit = (event) => {
    event.preventDefault()

    new OkDialog(
      "dialog_properties_header".localize,
      List("not_implemented".localize)
    )
      .renderToBody()
      .show()
  }

  private def jsPlayAllSelectedAction(action: ProjectAction): (Event) => Unit = (event) => {
    event.preventDefault()

    new SimpleModalDialog(
      <p>{"project_resume_dialog_content".localize}</p>,
      <h4 class={Dialog.Style.header.htmlClass}>{"are_you_sure".localize}</h4>,
      (dialog) => {
        dialog.close()
        NProgress.start()
        applyToAllSelected(action).foreach(_ => {
          NProgress.done(true)
        })
      },
      (dialog) => {dialog.close()}
    ).renderToBody().show()

  }

  private def applyAction(clientList: List[BoincClient], project: String, action: ProjectAction): Future[List[Boolean]] =
    Future.sequence(
      clientList.map(_.project(project, action).recover { case _: Exception => false })
    )

  private def applyToAll(project: String, action: ProjectAction): Future[List[Boolean]] =
    applyAction(dataset.now(project).map(_._1), project, action)

  private def applyToAllSelected(action: ProjectAction): Future[List[List[Boolean]]] =
    Future.sequence(
      getSelectedProjects.map(project => applyToAll(project, action))
    )

  private def getSelectedProjects: List[String] = {
    val result = new ListBuffer[String]()
    val boxes = dom.document.querySelectorAll("#swarm-project-data-table input[type='checkbox']")

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    boxes.forEach((node, _, _) => {
      if (node.asInstanceOf[HTMLInputElement].checked)
        result += node.asInstanceOf[HTMLInputElement].value
    })

    println(result)
    result.toList
  }

  private val jsSelectAllListener: (Event) => Unit = (event) => {
    event.preventDefault()
    val boxes = dom.document.querySelectorAll("#swarm-project-data-table input[type='checkbox']")

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    boxes.forEach((node, _, _) => node.asInstanceOf[HTMLInputElement].checked = true)
  }

  private[this] def updateCache(project: Project): String = {
    ProjectNameCache.save(project.url, project.name)
    project.name
  }

}
