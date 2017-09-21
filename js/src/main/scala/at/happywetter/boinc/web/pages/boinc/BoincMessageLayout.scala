package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, FetchResponseException}
import at.happywetter.boinc.web.css.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincMessageLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{DOMParser, HTMLElement}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
object BoincMessageLayout {

  object Style extends StyleSheet.Inline {
  import dsl._

  import scala.language.postfixOps

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
    import at.happywetter.boinc.web.hacks.NodeListConverter._
    NProgress.start()

    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

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
    NProgress.done(true)
  }

  private def renderNotices(client: BoincClient): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getAllNotices.map(notices => {
      dom.document.getElementById("client-notices").appendChild(
        ul(Style.noticeList,
          notices.reverse.map(notice =>
            li(
              div(
                if (notice.category == "client")
                  h4(if (notice.project.nonEmpty) notice.project + ": " else "", "notice_from_boinc".localize)
                else
                  h4(notice.title)
                ,
                p(convertContent(notice.description)),
                small(BoincFormater.convertDate(notice.createTime),
                  if (notice.link.nonEmpty)
                    a("read_more_link".localize, onclick := AppRouter.openExternal, href := notice.link)
                  else span()
                )
              )
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
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }


  override val path: String = "messages"

  private def convertContent(content: String) = {
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    // Parse HTML Input
    val root = new DOMParser().parseFromString(
      s"<div id='root'>${content.replace("\n","<br/>")}</div>",
      "text/html"
    )

    // Blacklist Filter for non-truested Stuff:
    root.getElementsByName("script").forEach((node, _, _) => root.removeChild(node))
    root.querySelectorAll("[onclick]").forEach((node, _, _) =>
      node.asInstanceOf[HTMLElement].removeAttribute("onclick")
    )

    // Cleanup for display
    val ret = root.getElementById("root")
    ret.removeAttribute("id")

    ret
  }
}
