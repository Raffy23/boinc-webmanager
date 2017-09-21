package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.Project
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincProjectLayout.Style
import at.happywetter.boinc.web.pages.component.dialog.{OkDialog, ProjectAddDialog}
import at.happywetter.boinc.web.pages.component.{BoincPageLayout, ContextMenu, Tooltip}
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.StatisticPlatforms
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.{Event, MouseEvent}

import scala.scalajs.js
import scalacss.internal.mutable.StyleSheet
import scalacss.ProdDefaults._

/**
  * Created by:
  *
  * @author Raphael
  * @version 02.08.2017
  */
object BoincProjectLayout {

  object Style extends StyleSheet.Inline {
    import dsl._

    val link = style(
      cursor.pointer,
      textDecoration := "none",
      color(c"#333")
    )

  }

}

class BoincProjectLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    client.getProjects.map(results => {
      import scalacss.ScalatagsCss._
      import scalatags.JsDom.all._

      root.appendChild(
        div( id := "projects",
          h2(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-tag"), "project_header".localize),
          div(style := "position:absolute;top:80px;right:20px;",
            new Tooltip("project_new_tooltip".localize,
              a(href := "#add-project", i(`class` := "fa fa-plus-square"), style := "color:#333;text-decoration:none;font-size:30px",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  //TODO: Use some cache ...
                  ClientManager.queryCompleteProjectList().foreach(data => {
                    new ProjectAddDialog(data, (url, username, password, name) => {
                      client.attachProject(url, username, password, name).map(result => {
                        NProgress.done(true)
                        onRender(client)

                        if(!result)
                          new OkDialog("dialog_error_header".localize, List("project_new_error_msg".localize), (_) => {
                            dom.document.getElementById("pad-username").asInstanceOf[HTMLElement].focus()
                          }).renderToBody().show()

                        result
                      })
                    }).renderToBody().show()

                    NProgress.done(true)
                  })
                }},
              ),
              textOrientation = Tooltip.Style.leftText
            ).render()
          ),
          table(TableTheme.table, TableTheme.table_lastrowsmall,
            thead(
              tr(
                th("table_project".localize), th("table_account".localize), th("table_team".localize),
                th("table_credits".localize), th("table_avg_credits".localize), th()
              )
            ),
            tbody(
              results.map(project => {
                tr(data("project-uri") := project.url, oncontextmenu := { (event: Event) => {
                  val contextMenu = new ContextMenu("project-"+results.indexOf(project)+"-context-menu")
                  project.guiURLs.foreach( url => {
                    contextMenu.addMenu(url.url, url.name, AppRouter.openExternal)
                  })

                  event.preventDefault()
                  contextMenu.renderToBody().display(event.asInstanceOf[MouseEvent])
                }},
                  td(a(updateCache(project), href := project.url, onclick := AppRouter.openExternal, Style.link), style := "max-width: 100px;"),
                  td(project.userName),
                  td(project.teamName),
                  td(project.userTotalCredit),
                  td(project.hostAvgCredit),
                  td(
                    new Tooltip(if (project.dontRequestWork) "project_allow_more_work".localize else "project_dont_allow_more_work".localize ,
                      a(href := "#change-project-state", i(`class` := s"fa fa-${if (project.dontRequestWork) "play" else "pause" }-circle-o"),
                        data("pause-work") := project.dontRequestWork, onclick := { (event: Event) => {
                          event.preventDefault()
                          NProgress.start()

                          val source = event.target.asInstanceOf[HTMLElement].parentNode.asInstanceOf[HTMLElement]
                          val state  = source.getAttribute("data-pause-work").toBoolean
                          val action = if (!state) ProjectAction.NoMoreWork else  ProjectAction.AllowMoreWork
                          source.classList.add("fa-spin")

                          boinc.project(project.url, action).map(result => {
                            NProgress.done(true)
                            source.classList.remove("fa-spin")

                            if (!result)
                              new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
                                .renderToBody().show()
                            else {
                              val tooltip = source.nextSibling
                              source.setAttribute("data-pause-work", (!state).toString)

                              if (!state) {
                                source.firstChild.asInstanceOf[HTMLElement].classList.add("fa-play-circle-o")
                                source.firstChild.asInstanceOf[HTMLElement].classList.remove("fa-pause-circle-o")
                                tooltip.textContent = "project_allow_more_work".localize
                              } else {
                                source.firstChild.asInstanceOf[HTMLElement].classList.add("fa-pause-circle-o")
                                source.firstChild.asInstanceOf[HTMLElement].classList.remove("fa-play-circle-o")
                                tooltip.textContent = "project_dont_allow_more_work".localize
                              }
                            }
                          }).recover {
                            case _: FetchResponseException =>
                              new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
                                .renderToBody().show()
                          }
                        }})
                    ).render(),

                    new Tooltip("project_refresh".localize,
                      a(href := "#refresh-project", i(`class` := "fa fa-fw fa-refresh", style := "font-size:20px"),
                      onclick := {
                        (event: Event) => {
                          event.preventDefault()
                          NProgress.start()

                          val source = event.target.asInstanceOf[HTMLElement]
                          source.classList.add("fa-spin")

                          boinc.project(project.url, ProjectAction.Update).map(result => {
                            NProgress.done(true)
                            if (!result)
                              new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
                                .renderToBody().show()

                            source.classList.remove("fa-spin")
                          }).recover {
                            case _: FetchResponseException =>
                              new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
                                .renderToBody().show()
                          }
                        }
                      })
                    ).render(),

                    new Tooltip("project_properties".localize,
                      a(href := "#project-properties", i(`class` := "fa fa-info-circle"),
                        onclick := {
                        (event: Event) => {
                          event.preventDefault()

                          //TODO: print some details
                          new OkDialog("workunit_dialog_properties".localize + " " + project.name,
                            List(
                              h4("project_dialog_general_header".localize, BoincClientLayout.Style.h4),
                              table(TableTheme.table,
                                tbody(
                                  tr(
                                    td(b("project_dialog_url".localize)),
                                    td(a(project.url, href := project.url, onclick := AppRouter.openExternal, Style.link))
                                  ),
                                  tr(td(b("login_username".localize)), td(project.userName)),
                                  tr(td(b("project_dialog_teamname".localize)), td(project.teamName)),
                                  tr(td(b("resource_share".localize)), td(project.resourceShare)),
                                  tr(td(b("disk_usage".localize)), td(BoincFormater.convertSize(project.desiredDiskUsage))),
                                  tr(td(b("project_dialog_cpid".localize)), td(project.cpid)),
                                  tr(td(b("project_dialog_host_id".localize)), td(project.hostID,
                                    span(style := "float: right",
                                      a(img(src := "/files/images/freedc_icon.png", alt := "freecd_icon"),
                                        href := StatisticPlatforms.freedc(project.cpid),
                                        onclick := { (event: Event) => {
                                          event.preventDefault()
                                          AppRouter.openExternalLink(StatisticPlatforms.freedc(project.cpid))
                                        }}
                                      ),
                                      a(img(src := "/files/images/boincstats_icon.png", alt := "boincstats_icon"),
                                        href := StatisticPlatforms.boincStats(project.cpid),
                                        onclick := { (event: Event) => {
                                          event.preventDefault()
                                          AppRouter.openExternalLink(StatisticPlatforms.boincStats(project.cpid))
                                        }}
                                      )
                                    ))
                                  ),
                                  tr(td(b("project_dialog_paused".localize)), td(project.dontRequestWork.localize)),
                                  tr(td(b("project_dialog_jobs_succ".localize)), td(project.jobSucc)),
                                  tr(td(b("project_dialog_jobs_err".localize)), td(project.jobErrors)),
                                )
                              ),
                              h4("project_dialog_credits_header".localize, BoincClientLayout.Style.h4),
                              table(TableTheme.table,
                                tbody(
                                  tr(td(b("project_dialog_credits_user".localize)), td(project.userTotalCredit)),
                                  tr(td(b("project_dialog_credits_uavg".localize)), td(project.userAvgCredit)),
                                  tr(td(b("project_dialog_credits_host".localize)), td(project.hostTotalCredit)),
                                  tr(td(b("project_dialog_credits_havg".localize)), td(project.hostAvgCredit)),
                                )
                              )
                            )
                          ).renderToBody().show()
                        }
                      })
                    ).render()
                  )
                )
              })
            )
          )
        ).render
      )
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }

    NProgress.done(true)
  }

  private[this] def updateCache(project: Project): String = {
    ProjectNameCache.save(project.url, project.name)
    project.name
  }

  override val path = "projects"
}
