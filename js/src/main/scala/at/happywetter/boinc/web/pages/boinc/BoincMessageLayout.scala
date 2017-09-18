package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.Message
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater}
import at.happywetter.boinc.web.css.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincMessageLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
object BoincMessageLayout {

  object Style extends StyleSheet.Inline {
  import scala.language.postfixOps
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

    val noticeList = style(
      padding.`0`,
      listStyle := "none",

      unsafeChild("li")(
        unsafeChild("h4")(
          BoincClientLayout.Style.pageHeader_small,
          fontSize(21 px)
        ),

        unsafeChild("p")(
          lineHeight(1.4923),
          marginTop(-2 px),

          unsafeChild("a")(
            color(c"#0039e6")
          )
        ),

        unsafeChild("small")(
          color(c"#888"),

          unsafeChild("a")(
            color(c"#0039e6"),
            marginLeft(7 px)
          )
        )
      )
    )

  }

}


class BoincMessageLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  override def onRender(client: BoincClient): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    root.appendChild(
      div(id := "messages",
        div(FloatingMenu.root,
          a("noties_menu_entry".localize, FloatingMenu.active, onclick := { (event: Event) => {
            event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
              node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
            })
            event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

            dom.window.document.getElementById("client-notices").asInstanceOf[HTMLElement].style="display:block"
            dom.window.document.getElementById("client-messages").asInstanceOf[HTMLElement].style="display:none"
          }}),
          a("message_menu_entry".localize, onclick := { (event: Event) => {
            event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
              node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
            })
            event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

            dom.window.document.getElementById("client-notices").asInstanceOf[HTMLElement].style="display:none"
            dom.window.document.getElementById("client-messages").asInstanceOf[HTMLElement].style="display:block"
          }})
        ),
        h3(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-envelope-o"), "messages_header".localize),
        div(id := "client-notices"),
        div(id := "client-messages", style := "display:none")
      ).render
    )

    renderNotices(client)
    renderMessages(client)
  }

  private def renderNotices(client: BoincClient): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getAllNotices.map(notices => {
      dom.document.getElementById("client-notices").appendChild(
        ul(Style.noticeList,
          notices.reverse.map(notice => {
            //TODO: Changes this, since it can render malicious code, server & core client must be trusted fully
            val content = dom.document.createElement("p")
            content.innerHTML = notice.description

            li(
              div(
                if (notice.category == "client")
                  h4(if (notice.project.nonEmpty) notice.project + ": " else "", "notice_from_boinc".localize)
                else
                  h4(notice.title)
                ,
                content,
                small(BoincFormater.convertDate(notice.createTime),
                  if (notice.link.nonEmpty)
                    a("read_more_link".localize, onclick := AppRouter.openExternal, href := notice.link)
                  else span()
                )
              )
            )
          })
        ).render
      )
    })
  }


  private def renderMessages(client: BoincClient): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getAllMessages.map(messages => {
      dom.document.getElementById("client-messages").appendChild(
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
