package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.{Message, Notice}
import at.happywetter.boinc.web.boincclient.BoincFormater.Implicits._
import at.happywetter.boinc.web.css.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.pages.boinc.BoincMessageLayout.Style
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{DOMParser, HTMLElement}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.Elem
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


class BoincMessageLayout extends BoincClientLayout {

  override val path: String = "messages"

  private val messages = Var(List.empty[Message])
  private val notices = Var(List.empty[Notice])


  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    NProgress.start()

    boinc
      .getMessages()
      .map(msg => messages := msg)
      .flatMap(_ =>
        boinc
          .getNotices()
          .map(notice => notices := notice)
      )
      .recover(ErrorDialogUtil.showDialog)
      .map(_ => NProgress.done(true))
  }

  override def render: Elem = {
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    <div id="messages">
      <div class={FloatingMenu.root.htmlClass}>
        <a class={FloatingMenu.active.htmlClass} onclick={ (event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
          })
          event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

          dom.window.document.getElementById("client-notices").asInstanceOf[HTMLElement].style="display:block"
          dom.window.document.getElementById("client-messages").asInstanceOf[HTMLElement].style="display:none"
        }}>
          {"noties_menu_entry".localize}
        </a>
        <a class={FloatingMenu.active.htmlClass} onclick={ (event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
          })
          event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

          dom.window.document.getElementById("client-notices").asInstanceOf[HTMLElement].style="display:none"
          dom.window.document.getElementById("client-messages").asInstanceOf[HTMLElement].style="display:block"
        }}>
          {"message_menu_entry".localize}
        </a>
      </div>

      <h3 class={BoincClientLayout.Style.pageHeader.htmlClass}>
        <i class="fa fa-envelope-o"></i>
        {"messages_header".localize}
      </h3>
      <div id="client-notices">
        <ul class={Style.noticeList.htmlClass}>
          {
            notices.map(_.map(notice =>
              <li>
                <div>
                  {
                    notice.category match {
                      case "client" =>
                        <h4>
                          {if (notice.project.nonEmpty) notice.project + ": " else ""}
                          {"notice_from_boinc".localize}
                        </h4>
                      case _ => <h4>{notice.title}</h4>
                    }
                  }
                  <p mhtml-onmount={jsOnContentMountAction(notice.description)}></p>
                  <small>
                    {notice.createTime.toDate}
                    {
                      if(notice.link.isEmpty) {
                        Some(
                          <a onclick={AppRouter.openExternal} href={notice.link}>
                            {"read_more_link".localize}
                          </a>
                        )
                      } else {
                        None
                      }
                    }
                  </small>
                </div>
              </li>
            ))
          }
        </ul>
      </div>
      <div id="client-messages" style="display:none">
        <table class={TableTheme.table.htmlClass}>
          <thead>
            <tr>
              <th>{"table_project".localize}</th>
              <th>{"table_time".localize}</th>
              <th>{"table_msg_content".localize}</th>
            </tr>
          </thead>
          <tbody>
            {
              messages.map(_.map(message =>
                <tr class={Style.tableRow.htmlClass} data-prio={message.priority.toString}>
                  <td>{message.project}</td>
                  <td class={Style.dateCol.htmlClass}>{message.time.toDate}</td>
                  <td>{message.msg}</td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>
    </div>
  }

  private def jsOnContentMountAction(content: String): (dom.html.Paragraph) => Unit = (p) => {
    p.appendChild(convertContent(content))
  }

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
