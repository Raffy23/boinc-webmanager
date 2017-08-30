package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.Project
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.component.{BoincPageLayout, SimpleModalDialog, Tooltip}
import at.happywetter.boinc.web.pages.{BoincClientLayout, LoginPage}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.ProjectNameCache
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement, HTMLSelectElement}

import scala.scalajs.js
import at.happywetter.boinc.web.util.I18N._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.08.2017
  */
class BoincProjectLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    client.getProjects.foreach(results => {
      import scalacss.ScalatagsCss._
      import scalatags.JsDom.all._

      root.appendChild(
        div( id := "projects",
          h2(BoincClientLayout.Style.pageHeader, "project_header".localize),
          div(style := "position:absolute;top:80px;right:20px;",
            new Tooltip("project_new_tooltip".localize,
              a(href := "#add-project", i(`class` := "fa fa-plus-square"), style := "color:#333;text-decoration:none;font-size:30px",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  //TODO: Use some cache ...
                  ClientManager.queryCompleteProjectList().foreach(data => {
                    new SimpleModalDialog(div(
                      table(TableTheme.table,
                        tbody(
                          tr(td("table_project".localize, style := "width:125px"),
                            td(
                              select(LoginPage.Style.input, style := "margin:0", id := "pad-project",
                                option(disabled, selected := "selected", "project_new_default_select".localize),
                                data.map(project => option(value := project._1, project._1)).toList,
                                onchange := { (event: Event) => {
                                  val element = data(event.target.asInstanceOf[HTMLSelectElement].value)
                                  dom.document.getElementById("pad-url").textContent = element.url
                                  dom.document.getElementById("pad-url").setAttribute("href", element.url)
                                  dom.document.getElementById("pad-general_area").textContent = element.general_area
                                  dom.document.getElementById("pad-description").textContent = element.description
                                  dom.document.getElementById("pad-home").textContent = element.home
                                }}
                              )
                            )
                          ),
                          tr(td("project_new_url".localize), td(a(id := "pad-url", onclick := {(event: Event) => {event.preventDefault(); dom.window.open(event.target.asInstanceOf[HTMLElement].getAttribute("href"),"_blank")}}))),
                          tr(td("project_new_general_area".localize), td(id := "pad-general_area")),
                          tr(td("project_new_desc".localize), td(id := "pad-description")),
                          tr(td("project_new_home".localize), td(id := "pad-home"))
                        )
                      ),
                      br(),
                      h4("project_new_userdata".localize),
                      table(style := "width: calc(100% - 20px)",
                        tbody(
                          tr(td("login_username".localize), td(input(LoginPage.Style.input, placeholder := "example@boinc-user.com", style := "margin:0", id := "pad-username"))),
                          tr(td("login_password".localize), td(input(LoginPage.Style.input, placeholder := "login_password".localize, `type` := "password", style := "margin:0", id := "pad-password"))),
                        )
                      ), br(), br()),
                      h2("project_new_addbtn".localize),
                      (dialog: SimpleModalDialog) => {
                        NProgress.start()

                        val select = dom.document.getElementById("pad-project").asInstanceOf[HTMLSelectElement]
                        val element = data(select.value)
                        val username = dom.document.getElementById("pad-username").asInstanceOf[HTMLInputElement].value
                        val password = dom.document.getElementById("pad-password").asInstanceOf[HTMLInputElement].value
                        client.attachProject(element.url, username, password, element.name).foreach(result => {
                          NProgress.done(true)
                          dialog.hide()
                          onRender(client)

                          //TODO: Use better Dialog
                          if(!result)
                            dom.window.alert("project_new_error_msg".localize)
                        })
                      },
                      (dialog: SimpleModalDialog) => {dialog.hide()}
                    ).renderToBody().show()

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
                tr(data("project-uri") := project.url,
                  td(updateCache(project), style := "max-width: 100px;"),
                  td(project.userName),
                  td(project.teamName),
                  td(project.userTotalCredit),
                  td(project.hostAvgCredit),
                  td(
                    new Tooltip(if (project.dontRequestWork) "project_allow_more_work".localize else "project_dont_allow_more_work".localize ,
                      a(href := "#change-project-state", i(`class` := s"fa fa-${if (project.dontRequestWork) "play" else "pause" }-circle-o"))
                    ).render(),

                    new Tooltip("project_refresh".localize,
                      a(href := "#refresh-project", i(`class` := "fa fa-fw fa-refresh", style := "font-size:20px"),
                      onclick := {
                        (event: Event) => {
                          event.preventDefault()
                          NProgress.start()

                          val source = event.target.asInstanceOf[HTMLElement]
                          source.classList.add("fa-spin")

                          boinc.project(project.url, ProjectAction.Update).foreach(result => {
                            NProgress.done(true)
                            if (!result) dom.window.alert("not_succ_action".localize)
                            else {
                              source.classList.remove("fa-spin")
                              //TODO: Change tooltip Text
                            }
                          })
                        }
                      }), tooltipId = Some("tooltip-"+project.name)
                    ).render(),

                    new Tooltip("project_properties".localize,
                      a(href := "#project-properties", i(`class` := "fa fa-info-circle"))
                    ).render()
                  )
                )
              })
            )
          )
        ).render
      )
    })

    NProgress.done(true)
  }

  private[this] def updateCache(project: Project): String = {
    ProjectNameCache.save(project.url, project.name)
    project.name
  }

}
