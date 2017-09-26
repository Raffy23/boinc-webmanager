package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.Project
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.helper.table.DataModelConverter._
import at.happywetter.boinc.web.helper.table.ProjectDataTableModel.ProjectTableRow
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincProjectLayout.Style
import at.happywetter.boinc.web.pages.component.dialog.{OkDialog, ProjectAddDialog}
import at.happywetter.boinc.web.pages.component.{BoincPageLayout, DataTable, Tooltip}
import at.happywetter.boinc.web.routes.{Hook, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
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
      textDecoration := "none",
      color(c"#333")
    )

    val firstRowFixedWith = style(
      unsafeChild("tbody > tr > td:first-child")(
        maxWidth(100 px)
      )
    )

  }
}

class BoincProjectLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  private var dataTable: DataTable[ProjectTableRow] = _


  override val routerHook = Some(new Hook() {
    override def before(done: js.Function0[Unit]): Unit = {
      NProgress.start()
      done()
    }

    override def after(): Unit = {}

    override def leave(): Unit = {
      dataTable.dispose()
    }

    override def already(): Unit = {
      dataTable.dispose()
    }
  })

  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    client.getProjects.map(results => {
      import scalacss.ScalatagsCss._
      import scalatags.JsDom.all._

      dataTable = new DataTable(List(
        ("table_project".localize, true),
        ("table_account".localize, true),
        ("table_team".localize, true),
        ("table_credits".localize, true),
        ("table_avg_credits".localize, true),
        ("", false)
      ), results, Some(Style.firstRowFixedWith))
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
                      }).recover{
                        case e => ErrorDialogUtil.showDialog(e); false
                      }
                    }).renderToBody().show()

                    NProgress.done(true)
                  })
                }},
              ),
              textOrientation = Tooltip.Style.leftText
            ).render()
          ),

          dataTable.component

        ).render
      )

      NProgress.done(true)
    }).recover(ErrorDialogUtil.showDialog)
  }

  override val path = "projects"
}
