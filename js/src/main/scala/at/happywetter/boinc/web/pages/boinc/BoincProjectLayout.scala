package at.happywetter.boinc.web.pages.boinc

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.HTMLElement
import scala.xml.Elem

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.pages.{BoincProjectStyle => Style}
import at.happywetter.boinc.web.model.DataModelConverter
import at.happywetter.boinc.web.model.DataModelConverter._
import at.happywetter.boinc.web.model.ProjectDataTableModel.ProjectTableRow
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.dialog.ProjectAddDialog
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.util.XMLHelper.toXMLTextNode

import mhtml.Var

/**
  * Created by:
  *
  * @author Raphael
  * @version 02.08.2017
  */
class BoincProjectLayout extends BoincClientLayout:
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val path = "projects"

  private val dataTable: DataTable[ProjectTableRow] = new DataTable[ProjectTableRow](
    List(
      ("table_project".localize, true),
      ("table_account".localize, true),
      ("table_team".localize, true),
      ("table_credits".localize, true),
      ("table_avg_credits".localize, true),
      ("", false)
    ),
    List.empty,
    List(
      TableTheme.table,
      TableTheme.lastRowSmall
    ),
    paged = true
  )

  override def render: Elem =
    implicit val implicitDataTable: DataTable[ProjectTableRow] = dataTable
    boinc.getProjects
      .map { projects =>
        dataTable.reactiveData := projects
        NProgress.done(true)
      }
      .recover(ErrorDialogUtil.showDialog)

    <div id="projects">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-tag" aria-hidden="true"></i>
        {"project_header".localize}
      </h2>

      <div class={Style.floatingHeadbar.htmlClass}>
        {
      new Tooltip(
        Var("project_new_tooltip".localize),
        <a href="#add-project" class={Style.floatingHeadbarButton.htmlClass} onclick={jsProjectAddAction}>
              <i class="fa fa-plus-square"></i>
            </a>
      ).toXML
    }
      </div>

      {dataTable.component}
    </div>

  private val jsProjectAddAction: (Event) => Unit = event => {
    event.preventDefault()
    NProgress.start()

    // TODO: Use some cache ...
    ClientManager
      .queryCompleteProjectList()
      .foreach(data => {
        lazy val projectAddDialog: ProjectAddDialog = new ProjectAddDialog(
          data,
          (url, username, password, name) => {

            boinc
              .attachProject(url, username, password, name)
              .map(result => {
                NProgress.done(true)

                if (!result)
                  new OkDialog("dialog_error_header".localize,
                               List("project_new_error_msg".localize),
                               _ => {
                                 projectAddDialog.focusUsernameFiled()
                               }
                  ).renderToBody().show()
                else {
                  implicit val implicitDataTable: DataTable[ProjectTableRow] = dataTable
                  boinc.getProjects.foreach(projects => dataTable.reactiveData := projects)
                }

                result
              })
              .recover { case e =>
                ErrorDialogUtil.showDialog(e);
                false
              }
          }
        )

        projectAddDialog.renderToBody().show()
        NProgress.done(true)
      })
  }
