package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.Project
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.component.{BoincPageLayout, ModalDialog, Tooltip}
import at.happywetter.boinc.web.pages.{BoincClientLayout, LoginPage}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.ProjectNameCache
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement, HTMLSelectElement}

import scala.scalajs.js

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
          h2(BoincClientLayout.Style.pageHeader, "Projekte: "),
          div(style := "position:absolute;top:80px;right:20px;",
            new Tooltip("Neues Projekt hinzufügen",
              a(href := "#add-project", i(`class` := "fa fa-plus-square"), style := "color:#333;text-decoration:none;font-size:30px",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  //TODO: Use some cache ...
                  ClientManager.queryCompleteProjectList().foreach(data => {
                    new ModalDialog(div(
                      table(TableTheme.table,
                        tbody(
                          tr(td("Projekt", style := "width:125px"),
                            td(
                              select(LoginPage.Style.input, style := "margin:0", id := "pad-project",
                                option(disabled, selected := "selected", "Bitte wählen Sie ein Projekt aus ..."),
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
                          tr(td("URL: "), td(a(id := "pad-url", onclick := {(event: Event) => {event.preventDefault(); dom.window.open(event.target.asInstanceOf[HTMLElement].getAttribute("href"),"_blank")}}))),
                          tr(td("Bereich:"), td(id := "pad-general_area")),
                          tr(td("Beschreibung: "), td(id := "pad-description")),
                          tr(td("Oranisation"), td(id := "pad-home"))
                        )
                      ),
                      br(),
                      h4("Benutzerdaten: "),
                      table(style := "width: calc(100% - 20px)",
                        tbody(
                          tr(td("Username"), td(input(LoginPage.Style.input, placeholder := "example@boinc-user.com", style := "margin:0", id := "pad-username"))),
                          tr(td("Password"), td(input(LoginPage.Style.input, placeholder := "Passwort", `type` := "password", style := "margin:0", id := "pad-password"))),
                        )
                      ), br(), br()),
                      h2("Projekt hinzufügen"),
                      (dialog: ModalDialog) => {
                        NProgress.start()

                        val select = dom.document.getElementById("pad-project").asInstanceOf[HTMLSelectElement]
                        val element = data(select.value)
                        val username = dom.document.getElementById("pad-username").asInstanceOf[HTMLInputElement].value
                        val password = dom.document.getElementById("pad-password").asInstanceOf[HTMLInputElement].value
                        client.attachProject(element.url, username, password, element.name).foreach(result => {
                          NProgress.done(true)
                          dialog.hide()
                          onRender(client)

                          if(!result)
                            dom.window.alert("Couldn't attach to Project!")
                        })
                      },
                      (dialog: ModalDialog) => {dialog.hide()}
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
                th("Projekt"), th("Konto"), th("Team"), th("Credits"), th("Durchnitt"), th()
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
                    Tooltip(if (project.dontRequestWork) "Neuen Aufgaben zulassen" else "Keine neue Aufgaben" ,
                      a(href := "#change-project-state", i(`class` := s"fa fa-${if (project.dontRequestWork) "play" else "pause" }-circle-o"))
                    ).render(),

                    Tooltip("Aktualisieren",
                      a(href := "#refresh-project", i(`class` := "fa fa-fw fa-refresh", style := "font-size:20px"),
                      onclick := {
                        (event: Event) => {
                          event.preventDefault()
                          NProgress.start()

                          val source = event.target.asInstanceOf[HTMLElement]
                          source.classList.add("fa-spin")

                          boinc.project(project.url, ProjectAction.Update).foreach(result => {
                            NProgress.done(true)
                            if (!result) dom.window.alert("Project Update was *not* successful!")
                            else {
                              source.classList.remove("fa-spin")
                            }
                          })
                        }
                      })
                    ).render(),

                    Tooltip("Eigenschaften",
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
