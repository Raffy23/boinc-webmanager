package at.happywetter.boinc.web.pages.swarm

import at.happywetter.boinc.shared.{BoincRPC, Project}
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincProjectLayout
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scalacss.internal.mutable.StyleSheet
import scalatags.JsDom
import scalacss.ProdDefaults._
import BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.web.pages.component.dialog._

import scala.collection.mutable.ListBuffer

/**
  * Created by: 
  *
  * @author Raphael
  * @version 14.09.2017
  */
object ProjectSwarmPage extends SwarmSubPage {

  object Style extends StyleSheet.Inline {
    import scala.language.postfixOps
    import dsl._

    val masterCheckbox = BoincSwarmPage.Style.masterCheckbox
    val checkbox = BoincSwarmPage.Style.checkbox
    val center = BoincSwarmPage.Style.center
    val button = BoincSwarmPage.Style.button
    val link = BoincProjectLayout.Style.link

    val top_nav_action = style(
      color(c"#333"),
      textDecoration := "none",
      fontSize(28 px),
      marginRight(5 px)
    )

  }


  private case class Account(userName: String, teamName: String, credits: Double)

  override def header: String = "project_header".localize

  override def render: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    val root = div(id := "swarm-project-content",
      div(style := "position:absolute;top:80px;right:20px;",
        new Tooltip("project_swarm_play".localize,
          a(href := "#apply-to_all-project", i(`class` := "fa fa-play-circle-o"),
            Style.top_nav_action,
            onclick := { (event: Event) => {
              event.preventDefault()

              new SimpleModalDialog(
                p("project_resume_dialog_content".localize),
                h4(Dialog.Style.header, "are_you_sure".localize),
                (dialog) => {
                  dialog.close()
                  NProgress.start()
                  applyToAllSelected(BoincRPC.ProjectAction.Resume).foreach(_ => {
                    NProgress.done(true)
                  })
                },
                (dialog) => {dialog.close()}
              ).renderToBody().show()

            }},
          ),
          textOrientation = Tooltip.Style.topText
        ).render(),
        new Tooltip("project_swarm_pause".localize,
          a(href := "#apply-to_all-project", i(`class` := "fa fa-pause-circle-o"),
            Style.top_nav_action,
            onclick := { (event: Event) => {
              event.preventDefault()

              new SimpleModalDialog(
                p("project_suspend_dialog_content".localize),
                h4(Dialog.Style.header, "are_you_sure".localize),
                (dialog) => {
                  dialog.close()
                  NProgress.start()
                  applyToAllSelected(BoincRPC.ProjectAction.Suspend).foreach(_ => {
                    NProgress.done(true)
                  })
                },
                (dialog) => {dialog.close()}
              ).renderToBody().show()

            }},
          ),
          textOrientation = Tooltip.Style.topText
        ).render(),
        new Tooltip("project_swarm_refresh".localize,
          a(href := "#apply-to_all-project", i(`class` := "fa fa-refresh"),
            Style.top_nav_action,
            onclick := { (event: Event) => {
              event.preventDefault()
              NProgress.start()

              applyToAllSelected(BoincRPC.ProjectAction.Update).foreach(_ => {
                NProgress.done(true)
              })
            }},
          ),
          textOrientation = Tooltip.Style.topText
        ).render(),
        new Tooltip("project_swarm_trash".localize,
          a(href := "#apply-to_all-project", i(`class` := "fa fa-trash-o"),
            Style.top_nav_action,
            onclick := { (event: Event) => {
              event.preventDefault()

              new SimpleModalDialog(
                p("project_remove_dialog_content".localize),
                h4(Dialog.Style.header, "are_you_sure".localize),
                (dialog) => {
                  dialog.close()
                  NProgress.start()
                  applyToAllSelected(BoincRPC.ProjectAction.Remove).foreach(_ => {
                    NProgress.done(true)
                  })
                },
                (dialog) => {dialog.close()}
              ).renderToBody().show()
            }},
          ),
          textOrientation = Tooltip.Style.topText
        ).render(),
        new Tooltip("project_new_tooltip".localize,
          a(href := "#add-project", i(`class` := "fa fa-plus-square"),
            style := "color:#333;text-decoration:none;font-size:30px",
            onclick := { (event: Event) => {
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
            }},
          ),
          textOrientation = Tooltip.Style.topText
        ).render()
      ),
    )
    NProgress.start()

    ClientManager.getClients.foreach(clients => {
      Future.sequence(
        clients
          .map(client =>
            client.getProjects
                  .map(_.map(project => (client, project)))
                  .recover{ case _: Exception => List() }
          )
      ).map(_.flatten.groupBy(_._2.url))
       .map(projects => {
         val parent = dom.document.getElementById("swarm-project-content")
         //val hasEveryClient = projects.map { case (name, data) => (name, data.size == clients.size) }

         parent.appendChild(
         table(TableTheme.table, id := "swarm-project-data-table",
           thead(
             tr(BoincClientLayout.Style.in_text_icon,
               th(a(Style.masterCheckbox, i(`class` := "fa fa-check-square-o"), href := "#select-all", onclick := selectAllListener),
                 "table_project".localize), th("table_hosts".localize),
               th("table_account".localize),
               th("table_team".localize),
               th("table_credits".localize),
               th(style := "width:1.5em")
             )
           ),
           tbody(
             projects.map{ case(url, data) => {
               val project = data.head._2
               val accounts = data.map { case (_, project) => Account(project.userName, project.teamName, project.userAvgCredit) }
               val creditsRange = (accounts.map(_.credits).min, accounts.map(_.credits).max)

               tr(
                 td(input(Style.checkbox, `type` := "checkbox", value := url),
                   a(updateCache(project), href := project.url, onclick := AppRouter.openExternal, Style.link), style := "max-width: 100px;"),
                 td(data.size, style := "text-align:center"),
                 td(accounts.map(t => t.userName).distinct.map(t => List(span(t), br()))),
                 td(accounts.map(t => t.teamName).distinct.map(t => List(span(t), br()))),
                 td(creditsRange._1 + " - " + creditsRange._2),
                 td(
                   new Tooltip("project_properties".localize,
                     a(href := "#project-properties", i(`class` := "fa fa-info-circle"),
                       onclick := {
                         (event: Event) => {
                           event.preventDefault()
                         }
                       }
                     ), textOrientation = Tooltip.Style.leftText
                   ).render()
                 )
               )
             }}.toList
           )
         ).render
         )

         NProgress.done(true)
       })
    })

    root
  }


  private def applyAction(clientList: List[BoincClient], project: String, action: ProjectAction): Future[List[Boolean]] =
    Future.sequence(
      clientList.map(_.project(project, action).recover { case _: Exception => false })
    )

  private def applyToAll(project: String, action: ProjectAction): Future[List[Boolean]] =
    ClientManager.getClients.flatMap(cl => applyAction(cl, project, action))

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

  private val selectAllListener: js.Function1[Event, Unit] = (event) => {
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
