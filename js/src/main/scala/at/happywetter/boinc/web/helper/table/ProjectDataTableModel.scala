package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.Project
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincProjectLayout
import at.happywetter.boinc.web.pages.component.DataTable.{DoubleColumn, StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{ContextMenu, DataTable, Tooltip}
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{ErrorDialogUtil, StatisticPlatforms}
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.Event
import rx.{Ctx, Rx, Var}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.09.2017
  */
object ProjectDataTableModel {


  class ReactiveProject(val data: Project) {
    val dontRequestWork: Var[Boolean] = Var(data.dontRequestWork)
  }

  class ProjectTableRow(val project: ReactiveProject)(implicit boinc: BoincClient,ctx: Ctx.Owner) extends DataTable.TableRow {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    override val columns: List[DataTable.TableColumn] = List(
      new TableColumn(Rx {
        a(updateCache(project.data), href := project.data.url, onclick := AppRouter.openExternal, BoincProjectLayout.Style.link)
      }, this) {
        override def compare(that: TableColumn): Int = project.data.name.compare(that.datasource.asInstanceOf[ProjectTableRow].project.data.name)
      },
      new StringColumn(Rx {project.data.userName}),
      new StringColumn(Rx {project.data.teamName}),
      new DoubleColumn(Rx {project.data.userTotalCredit}),
      new DoubleColumn(Rx {project.data.hostAvgCredit}),
      new TableColumn( Rx {div(
        new Tooltip(if (project.dontRequestWork()) "project_allow_more_work".localize else "project_dont_allow_more_work".localize ,
          a(href := "#change-project-state", i(`class` := s"fa fa-${if (project.dontRequestWork.now) "play" else "pause" }-circle-o"),
            data("pause-work") := project.dontRequestWork.now, onclick := { (event: Event) => {
              event.preventDefault()
              NProgress.start()

              val action = if (!project.dontRequestWork.now) ProjectAction.NoMoreWork else  ProjectAction.AllowMoreWork


              boinc.project(project.data.url, action).map(result => {
                NProgress.done(true)

                if (!result)
                  new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
                    .renderToBody().show()
                else {
                  project.dontRequestWork() = !project.dontRequestWork.now
                }
              }).recover(ErrorDialogUtil.showDialog)
            }})
        ).render(),

        new Tooltip("project_refresh".localize,
          a(href := "#refresh-project", i(`class` := "fa fa-fw fa-refresh", style := "font-size:20px"),
            onclick := {
              (event: Event) => {
                event.preventDefault()
                NProgress.start()

                boinc.project(project.data.url, ProjectAction.Update).map(result => {
                  NProgress.done(true)

                  if (!result)
                    new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
                      .renderToBody().show()

                }).recover(ErrorDialogUtil.showDialog)
              }
            })
        ).render(),

        new Tooltip("project_properties".localize,
          a(href := "#project-properties", i(`class` := "fa fa-info-circle"),
            onclick := {
              (event: Event) => {
                event.preventDefault()

                //TODO: print some details
                new OkDialog("workunit_dialog_properties".localize + " " + project.data.name,
                  List(
                    h4("project_dialog_general_header".localize, BoincClientLayout.Style.h4),
                    table(TableTheme.table,
                      tbody(
                        tr(
                          td(b("project_dialog_url".localize)),
                          td(a(project.data.url, href := project.data.url, onclick := AppRouter.openExternal, BoincProjectLayout.Style.link))
                        ),
                        tr(td(b("login_username".localize)), td(project.data.userName)),
                        tr(td(b("project_dialog_teamname".localize)), td(project.data.teamName)),
                        tr(td(b("resource_share".localize)), td(project.data.resourceShare)),
                        tr(td(b("disk_usage".localize)), td(BoincFormater.convertSize(project.data.desiredDiskUsage))),
                        tr(td(b("project_dialog_cpid".localize)), td(project.data.cpid)),
                        tr(td(b("project_dialog_host_id".localize)), td(project.data.hostID,
                          span(style := "float: right",
                            a(img(src := "/files/images/freedc_icon.png", alt := "freecd_icon"),
                              href := StatisticPlatforms.freedc(project.data.cpid),
                              onclick := { (event: Event) => {
                                event.preventDefault()
                                AppRouter.openExternalLink(StatisticPlatforms.freedc(project.data.cpid))
                              }}
                            ),
                            a(img(src := "/files/images/boincstats_icon.png", alt := "boincstats_icon"),
                              href := StatisticPlatforms.boincStats(project.data.cpid),
                              onclick := { (event: Event) => {
                                event.preventDefault()
                                AppRouter.openExternalLink(StatisticPlatforms.boincStats(project.data.cpid))
                              }}
                            )
                          ))
                        ),
                        tr(td(b("project_dialog_paused".localize)), td(project.dontRequestWork().localize)),
                        tr(td(b("project_dialog_jobs_succ".localize)), td(project.data.jobSucc)),
                        tr(td(b("project_dialog_jobs_err".localize)), td(project.data.jobErrors)),
                      )
                    ),
                    h4("project_dialog_credits_header".localize, BoincClientLayout.Style.h4),
                    table(TableTheme.table,
                      tbody(
                        tr(td(b("project_dialog_credits_user".localize)), td(project.data.userTotalCredit)),
                        tr(td(b("project_dialog_credits_uavg".localize)), td(project.data.userAvgCredit)),
                        tr(td(b("project_dialog_credits_host".localize)), td(project.data.hostTotalCredit)),
                        tr(td(b("project_dialog_credits_havg".localize)), td(project.data.hostAvgCredit)),
                      )
                    )
                  )
                ).renderToBody().show()
              }
            })
        ).render()
      )}, this) {
        override def compare(that: TableColumn): Int = ???
      }

    )

    override val contextMenuHandler: js.Function1[Event, Unit] = (event) => {
      val contextMenu = new ContextMenu("project-"+project.data.hashCode()+"-context-menu")
      project.data.guiURLs.foreach( url => {
        contextMenu.addMenu(url.url, url.name, AppRouter.openExternal)
      })

      event.preventDefault()
      contextMenu.renderToBody().display(event.asInstanceOf[MouseEvent])
    }
  }

  def convert(project: Project)(implicit boinc: BoincClient): ProjectTableRow = Rx.unsafe {
    new ProjectTableRow(new ReactiveProject(project))
  }.now

  private[this] def updateCache(project: Project): String = {
    ProjectNameCache.save(project.url, project.name)
    project.name
  }
}
