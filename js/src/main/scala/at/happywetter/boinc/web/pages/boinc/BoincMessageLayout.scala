package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.Message
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincMessageLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.util.I18N._

import scala.scalajs.js
import scalacss.internal.mutable.StyleSheet
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
object BoincMessageLayout {

  object Style extends StyleSheet.Inline {

    import dsl._

    val dateCol = style(
      whiteSpace.nowrap
    )

    val tableRow = style(
      &.attr("data-prio", "2")(
        //fontWeight.bold,
        color(c"#ff1a1a")
      ),
      &.attr("data-prio", "3")(
        fontWeight.bold,
        color(c"#ff1a1a")
      )
    )

  }

}


class BoincMessageLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getAllMessages.map(messages => {

      root.appendChild(
        div(id := "messages",
          h3(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-envelope-o"), "messages_header".localize),
          div(
            table(TableTheme.table,
              thead(
                tr(
                  th("table_project".localize),
                  //th("table_priority".localize),
                  th("table_time".localize),
                  th("table_msg_content".localize)
                )
              ),
              tbody(
                messages.map(msg => {
                  tr(Style.tableRow, data("prio") := msg.priority,
                    td(msg.project),
                    //td(Style.dateCol, priToStr(msg.priority)),
                    td(Style.dateCol, BoincFormater.convertDate(msg.time)),
                    td(msg.msg)
                  )
                })
              )
            )
          )
        ).render
      )
    })

  }

  override val path: String = "messages"

  def priToStr(pri: Int): String = Message.Priority(pri) match {
    case Message.Priority.Info => "msg_type_info".localize
    case Message.Priority.InternalError => "msg_type_error".localize
    case Message.Priority.SchedulerAlert => "msg_type_alert".localize
    case Message.Priority.UserAlert => "msg_type_user_alert".localize
    case x => x.toString
  }

}
