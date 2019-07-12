package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.helper.XMLHelper.toXMLTextNode
import at.happywetter.boinc.web.helper.table.DataModelConverter._
import at.happywetter.boinc.web.helper.table.ProjectDataTableModel.ProjectTableRow
import at.happywetter.boinc.web.pages.boinc.BoincProjectLayout.Style
import at.happywetter.boinc.web.pages.component.dialog.{OkDialog, ProjectAddDialog}
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.xml.Elem
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet

/**
  * Created by:
  *
  * @author Raphael
  * @version 02.08.2017
  */
object BoincProjectLayout {

  object Style extends StyleSheet.Inline {
    import dsl._

    import scala.language.postfixOps

    val link = style(
      cursor.pointer,
      textDecoration := none,
      color(c"#333")
    )

    val firstRowFixedWith = style(
      unsafeChild("tbody > tr > td:first-child")(
        maxWidth(100 px)
      )
    )

    val floatingHeadbar = style(
      position.absolute,
      top(80 px),
      right(20 px)
    )

    val floatingHeadbarButton = style(
      color(c"#333"),
      textDecoration := none,
      fontSize(30 px),
      cursor.pointer
    )

  }
}

class BoincProjectLayout extends BoincClientLayout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val path = "projects"

  private var dataTable: DataTable[ProjectTableRow] = new DataTable[ProjectTableRow](
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
      TableTheme.table.htmlClass,
      TableTheme.table_lastrowsmall.htmlClass
    )
  )


  override def render: Elem = {
    boinc.getProjects.foreach(projects => dataTable.reactiveData := projects)

    <div id="projects">
      <h2 class={BoincClientLayout.Style.pageHeader.htmlClass}>
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
  }

  private val jsProjectAddAction: (Event) => Unit = (event) => {
    event.preventDefault()
    NProgress.start()

      //TODO: Use some cache ...
      ClientManager.queryCompleteProjectList().foreach(data => {
        new ProjectAddDialog(data, (url, username, password, name) => {
          boinc.attachProject(url, username, password, name).map(result => {
            NProgress.done(true)

            if(!result)
              new OkDialog("dialog_error_header".localize, List("project_new_error_msg".localize), (_) => {
                dom.document.getElementById("pad-username").asInstanceOf[HTMLElement].focus()
              }).renderToBody().show()
            else
              boinc.getProjects.foreach(projects => dataTable.reactiveData := projects)

            result
          }).recover{
            case e => ErrorDialogUtil.showDialog(e); false
          }
        }).renderToBody().show()

        NProgress.done(true)
    })
  }
}
