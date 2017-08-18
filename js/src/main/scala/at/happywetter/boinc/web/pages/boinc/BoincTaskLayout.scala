package at.happywetter.boinc.web.pages.boinc
import at.happywetter.boinc.shared.BoincRPC.WorkunitAction
import at.happywetter.boinc.shared.{Result, Workunit}
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, ClientCacheHelper, ClientManager}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.{BoincPageLayout, ContextMenu, ModalDialog, Tooltip}
import at.happywetter.boinc.web.routes.{Hook, NProgress}
import at.happywetter.boinc.web.storage.{AppSettingsStorage, ProjectNameCache, TaskSpecCache}
import org.scalajs.dom
import org.scalajs.dom.{Event, MouseEvent}
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Date
import scalatags.JsDom
import scalatags.JsDom.TypedTag

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
class BoincTaskLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private var refreshHandle: Int = _
  private var timeUpdateHandle: Int = _
  private var fullSyncHandle: Int = _

  override def onRender(client: BoincClient): Unit = onViewRender((node) => {root.appendChild(node.render)})

  private def onViewRender(renderAction: (TypedTag[dom.html.Div]) => Unit): Unit = {
    val projectUris = new mutable.TreeSet[String]()

    boinc.getTasks(active = false).foreach(results => {
      val sortedResults = results.sortBy(f => f.activeTask.map(t => -t.done).getOrElse(0D))
      renderAction(renderView(projectUris, sortedResults))

      updateProjectNames(projectUris)
      updateWUNames()
    })
  }

  private def renderView(projectUris: mutable.Set[String], results: List[Result]): TypedTag[dom.html.Div] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div( id := "workunits",
      h2(BoincClientLayout.Style.pageHeader, "Workunits: "),
      table(TableTheme.table, TableTheme.table_lastrowsmall,
        thead(
          tr(
            th("Projekt"), th("Fortschritt"),th("Status"), th("Vergangen"), th("Verbleiben"), th("Ablaufdatum"), th("Andwendung"), th()
          )
        ),
        tbody(
          results.map(result => {
            tr( data("wu-id") := result.wuName,
              data("wu-state") := result.activeTask.map(t => t.activeTaskState).getOrElse(0),

              td(result.project, data("project-uri") := {
                projectUris.add(result.project)
                result.project
              }),
              td(BoincClientLayout.Style.progressBar,
                JsDom.tags2.progress(
                  value := result.activeTask.map(t => t.done).getOrElse(0D),
                  max := 1,
                ),
                span(style := "float:right", result.activeTask.map(t => t.done*100).getOrElse(0D).toString.split("\\.")(0) + " %")
              ),
              td(data("extra-flags") := "", prettyPrintStatus(result, inital = true)),
              td(BoincFormater.convertTime(result.activeTask.map(t => t.time).getOrElse(0D))),
              td(BoincFormater.convertTime(result.remainingCPU)),
              td(BoincFormater.convertDate(new Date(result.reportDeadline*1000))),
              td(data("wu-name") := result.wuName, result.wuName),
              td(
                Tooltip(if(result.supsended) "Fortsetzen" else "Anhalten",
                  a(href:="#", onclick := {
                    (event: Event) => {
                      event.preventDefault()
                      NProgress.start()

                      val source = event.target.asInstanceOf[HTMLElement].parentNode.asInstanceOf[HTMLElement]

                      if (source.getAttribute("data-in-process") == "false") {
                        source.setAttribute("data-in-process", "true")
                        source.firstChild.asInstanceOf[HTMLElement].classList.remove(s"fa-${ if(result.supsended) "play" else "pause" }-circle-o")
                        source.firstChild.asInstanceOf[HTMLElement].classList.add("fa-spinner")
                        source.firstChild.asInstanceOf[HTMLElement].classList.add("fa-spin")
                        source.firstChild.asInstanceOf[HTMLElement].classList.add("fa-fw")

                        val state = source.getAttribute("data-suspended").toBoolean
                        boinc.workunit(result.project, result.name, if (state) WorkunitAction.Resume else WorkunitAction.Suspend).onComplete(f => f.fold( (e) => e.printStackTrace(),
                          response => {
                            if (!response) {
                              dom.window.alert("Action was not successful!")
                            } else {
                              source.setAttribute("data-in-process", "false")
                              source.setAttribute("data-suspended", (!state).toString)
                              source.firstChild.asInstanceOf[HTMLElement].classList.add(s"fa-${if (!state) "play" else "pause"}-circle-o")
                              source.firstChild.asInstanceOf[HTMLElement].classList.remove("fa-spinner")
                              source.firstChild.asInstanceOf[HTMLElement].classList.remove("fa-spin")
                              source.firstChild.asInstanceOf[HTMLElement].classList.remove("fa-fw")
                              NProgress.done(true)
                            }
                          }
                        ))
                      } else {
                        NProgress.done(true)
                      }
                    }
                  },
                  i(`class` := s"fa fa-${ if(result.supsended) "play" else "pause" }-circle-o"), data("suspended") := result.supsended, data("in-process") := "false")
                ).render(),

                Tooltip("Abbrechen",
                  a(href:="#", i(`class` := "fa fa-stop-circle-o"),
                    onclick := {
                    (event: Event) => {
                      event.preventDefault()

                      new ModalDialog(
                        bodyElement = div(
                          "Sind Sie sicher das diese Workunit abgebrochen werden soll?",br(),
                          "Dabei geht jgendlicher Fortschritt verloren und dem Projektserver wird mitgeteilt dass die Workunit abgebrochen wurde",br(),p(
                            b("Details: "),br(),
                            table(
                              tbody(
                                tr(td("Projekt: "), td(result.project)),
                                tr(td("Name: "), td(result.name)),
                                tr(td("Verbleibende Zeit: "), td(BoincFormater.convertTime(result.remainingCPU)))
                              )
                            )
                          )
                        ),
                        okAction = (dialog: ModalDialog) => {
                          dialog.hide()
                          boinc.workunit(result.project, result.name, WorkunitAction.Suspend)
                          dom.window.alert("WU wurde nur Pausiert!")
                        },
                        abortAction = (dialog: ModalDialog) => {dialog.hide()},
                        headerElement = div(h3("Workunit Abbrechen?"))
                      ).renderToBody().show()
                    }
                  }
                  )
                ).render(),

                Tooltip("Eigenschaften",
                  a(href:="#", i(`class` := "fa fa-info-circle"),
                    onclick := {
                      (event: Event) => {
                        event.preventDefault()

                        new ModalDialog(
                          bodyElement = div("TODO: Details anzeigen"),
                          okAction = (dialog: ModalDialog) => {dialog.hide()},
                          abortAction = (dialog: ModalDialog) => {dialog.hide()},
                          headerElement = div(h3("Eigenschaften: ")),
                          abortLabel = "Schließen",
                          okLabel = ""
                        ).renderToBody().show()
                      }
                    }
                  )
                ).render()
              )
            )
          })
        )
      )
    )
  }

  private def renderStatusExtraFlags(wuName: String): Unit = {
    def updateTaskFlags(wu: Workunit): Unit = {
      TaskSpecCache.get(boincClientName, wu.appName).foreach(app => {

        if (app.isDefined) {
          val node = dom.document.querySelector(s"div[id='workunits'] > table > tbody > tr[data-wu-id='$wuName'] > td:nth-child(3)").asInstanceOf[HTMLElement]

          if (app.get.nonCpuIntensive) {
            node.setAttribute("data-extra-flags", "nci")
            node.textContent = node.textContent + " (nicht CPU-intensiv)"
          }

          if (app.get.version.avgCpus > 1) {
            node.setAttribute("data-cpu", app.get.version.avgCpus.toString)
            node.setAttribute("data-extra-flags", node.getAttribute("data-extra-flags") + ",cpu")
            node.textContent = node.textContent + s" (${app.get.version.avgCpus} CPUs)"
          }
        }
      })
    }

    AppSettingsStorage.get(boincClientName, wuName).foreach(wu => {
      if (wu.isDefined) {
        updateTaskFlags(wu.get)
      } else {
        ClientCacheHelper.updateClientCache(boinc, (_) => {
          dom.window.setTimeout(() => {renderStatusExtraFlags(wuName)}, 10)
        })
      }
    })

  }

  private def prettyPrintStatus(result: Result, inital: Boolean): String = {
    if (inital) renderStatusExtraFlags(result.wuName)

    Result.State(result.state) match {
      case Result.State.Result_New => "Neu"
      case Result.State.Result_Aborted => "Abgebrochen"
      case Result.State.Result_Compute_Error => "Berechnungsfehler"
      case Result.State.Result_Files_Downloaded =>
        result.activeTask.map(task => Result.ActiveTaskState(task.activeTaskState) match {
          case Result.ActiveTaskState.PROCESS_EXECUTING => {
            var base = "Aktiv"

            if (!inital) {
              val node = dom.document.querySelector(s"div[id='workunits'] > table > tbody > tr[data-wu-id='${result.wuName}'] > td:nth-child(3)").asInstanceOf[HTMLElement]
              if (node.getAttribute("data-extra-flags").contains("nci")) base = base + " (nicht CPU intensiv)"
              if (node.getAttribute("data-extra-flags").contains("cpu")) {
                base = base + "(" + node.getAttribute("data-cpu") + " CPUs)"
              }
            }

            base
          }
          case Result.ActiveTaskState.PROCESS_ABORTED => "Abgebrochen"
          case Result.ActiveTaskState.PROCESS_SUSPENDED => if(result.supsended) "Angehalten" else "Verdrängt"
          case Result.ActiveTaskState.PROCESS_EXITED => "Beendet"
          case Result.ActiveTaskState.PROCESS_UNINITIALIZED => "Verdrängt"
          case state => state.toString
        }).getOrElse("Bereit")
      case Result.State.Result_Files_Uploaded => "Meldebereit"
      case Result.State.Result_Files_Uploading => "Lädt hoch"
      case Result.State.Result_File_Downloading => "Downloading"
      case Result.State.Result_Upload_Failed => "Upload Fehlgeschlagen!"
    }
  }

  private def updateActiveTasks(): Unit = {
    val client = ClientManager.clients(boincClientName)
    //lastRefresh = new Date()

    client.getTasks().foreach(result => {
      result.foreach(task => {
        val node = dom.document.querySelector(s"div[id='workunits'] > table > tbody > tr[data-wu-id='${task.wuName}']")

        // Update Progressbar
        node.childNodes.item(1).firstChild.asInstanceOf[HTMLElement].setAttribute("value", task.activeTask.get.done.toString)
        node.childNodes.item(1).firstChild.nextSibling.textContent = (task.activeTask.get.done *100).toString.split("\\.")(0) + " %"

        node.childNodes.item(2).textContent = prettyPrintStatus(task, inital = false)
        node.childNodes.item(3).textContent = BoincFormater.convertTime(task.activeTask.map(t => t.time).getOrElse(0D))
        node.childNodes.item(4).textContent = BoincFormater.convertTime(task.remainingCPU)
      })
    })
  }

  private var lastRefresh = new Date()
  private def updateActiveTaskTimes(): Unit = {
    val tasks = dom.document.querySelectorAll(s"div[id='workunits'] > table > tbody > tr[data-wu-state='1']")
    val now = new Date()

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    tasks.forEach((node, _, _) => {
      val date = BoincFormater.convertTime(node.childNodes.item(3).firstChild.textContent) + (now.getTime() - lastRefresh.getTime()) / 1000
      node.childNodes.item(3).firstChild.textContent = BoincFormater.convertTime(date)
    })

    lastRefresh = now
  }

  private def syncTaskViewWithServer(): Unit = {
    NProgress.start()

    //TODO: Do a smart update of table rows, not view re-rendering
    val oldNode = dom.document.getElementById("workunits")
    onViewRender((newNode) => {root.replaceChild(newNode.render, oldNode)})
  }

  private def updateProjectNames(projectUris: mutable.Set[String]): Unit = {
    Future.sequence(
      projectUris.map(uri =>
        ProjectNameCache.get(uri).map(query => {
          query.exists(name => {
            replaceProjectNames(uri, name)
            true
          })
        })
      )
    ).map(result => result.find(value => !value))
      .onComplete(result => result.fold(
        (error) => {
          error.printStackTrace()
          NProgress.done(true)
        },

        (cacheMiss) => {
          if (cacheMiss.isDefined) {
            boinc.getProjects.foreach(projects => {
              projects.foreach(project => {
                ProjectNameCache.save(project.url, project.name)

                if (projectUris.contains(project.url))
                  replaceProjectNames(project.url, project.name)
              })
            })
          }

          NProgress.done(true)
        }
      ))
  }

  private def updateWUNames(): Unit = {
    val nodes = dom.document.querySelectorAll(s"div[id='workunits'] > table > tbody > tr > td[data-wu-name]")
    val names = new ListBuffer[String]

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    nodes.forEach((node,_,_) => names += node.textContent)


    def replaceNames(names: List[String]): Unit = {
      val futures = names.map(name => AppSettingsStorage.get(boincClientName, name))
        .map(result => result.map(wuop =>
          wuop.exists(wu => {
            val node = dom.document.querySelector(s"div[id='workunits'] > table > tbody > tr > td[data-wu-name='${wu.name}']")

            TaskSpecCache.get(boincClientName, wu.appName).onComplete(t => t.fold(
              (error) => { error.printStackTrace(); node.textContent = wu.appName},
              (data) => node.textContent = data.map(app => app.userFriendlyName).getOrElse(wu.appName)
            ))

            true
          })
        ))

      Future
        .sequence(futures)
        .map(cacheState => cacheState.find(cacheMiss => !cacheMiss))
        .onComplete(t => t.fold(
          (error) => {
            error.printStackTrace()
            NProgress.done(true)
          },

          (cacheMiss) => {
            if (cacheMiss.isDefined) {
              ClientCacheHelper.updateClientCache(boinc, (_) => {replaceNames(names)})
            }

            NProgress.done(true)
          }
        ))
    }

    if (TaskSpecCache.isCacheValid(boincClientName)) replaceNames(names.toList)
    else ClientCacheHelper.updateClientCache(boinc, (_) => {replaceNames(names.toList)})
  }

  private def replaceProjectNames(uri: String, name: String): Unit = {
    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    val nodeList = dom.document.querySelectorAll(s"div[id='workunits'] > table > tbody td[data-project-uri='$uri']")
    nodeList.forEach((node,_,_) => {node.textContent = name})
  }

  override val routerHook = Some(new Hook {
    override def before(done: js.Function0[Unit]): Unit = {
      NProgress.start()
      done()
    }

    override def after(): Unit = {
      refreshHandle = dom.window.setInterval(() => updateActiveTasks(), 5000)
      timeUpdateHandle = dom.window.setInterval(() => updateActiveTaskTimes(), 1000)
      fullSyncHandle = dom.window.setInterval(() => syncTaskViewWithServer(), 600000)
    }

    override def leave(): Unit = {
      dom.window.clearInterval(refreshHandle)
      dom.window.clearInterval(timeUpdateHandle)
      dom.window.clearInterval(fullSyncHandle)
    }

    override def already(): Unit = {
      syncTaskViewWithServer()
    }
  })
}
