package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.{Message, Notice}
import at.happywetter.boinc.web.boincclient.BoincFormater.Implicits._
import at.happywetter.boinc.web.css.CSSIdentifier
import at.happywetter.boinc.web.css.definitions.components.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle, BoincMessageStyle => Style}
import at.happywetter.boinc.web.helper.table.MessageTableModel.MessageTableRow
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.storage.MessageCache
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{DOMParser, HTMLElement}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.xml.Elem
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.table.DataModelConverter._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
class BoincMessageLayout extends BoincClientLayout {

  override val path: String = "messages"

  private val messages = Var(List.empty[Message])
  private val notices = Var(List.empty[Notice])

  private val messageTable = new DataTable[MessageTableRow](
    headers = List(
      ("table_project".localize, true),
      ("table_time".localize, true),
      ("table_msg_content".localize, false)
    ),
    tableStyle = List(TableTheme.table),
    paged = true
  )


  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    NProgress.start()

    MessageCache.getLastSeqNo(boincClientName).map { lastSavedSeqNo =>
      boinc
        .getMessages(lastSavedSeqNo)
        .flatMap { messages =>
          MessageCache.save(boincClientName, messages)
        }.map { _ =>
          MessageCache.get(boincClientName).toRx(List.empty).map(messageTable.reactiveData := _)
       }.flatMap(_ =>
          boinc
            .getNotices()
            .map(notice => notices := notice)
        )
        .recover(ErrorDialogUtil.showDialog)
        .map(_ => NProgress.done(true))

    }
    .recover(ErrorDialogUtil.showDialog)
    .map(_ => NProgress.done(true))

  }

  override def render: Elem = {
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    <div id="messages">
      <div class={FloatingMenu.root.htmlClass}>
        <a class={FloatingMenu.active.htmlClass} onclick={ (event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            if (!js.isUndefined(node.asInstanceOf[HTMLElement].classList))
              node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
          })
          event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

          dom.window.document.getElementById("client-notices").asInstanceOf[HTMLElement].style="display:block"
          dom.window.document.getElementById("client-messages").asInstanceOf[HTMLElement].style="display:none"
        }}>
          {"noties_menu_entry".localize}
        </a>
        <a onclick={ (event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            if (!js.isUndefined(node.asInstanceOf[HTMLElement].classList))
              node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
          })
          event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

          dom.window.document.getElementById("client-notices").asInstanceOf[HTMLElement].style="display:none"
          dom.window.document.getElementById("client-messages").asInstanceOf[HTMLElement].style="display:block"
        }}>
          {"message_menu_entry".localize}
        </a>
      </div>

      <h3 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-envelope" aria-hidden="true"></i>
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
                        <h4 class={BoincClientStyle.pageHeaderSmall.htmlClass}>
                          {if (notice.project.nonEmpty) notice.project + ": " else ""}
                          {"notice_from_boinc".localize}
                        </h4>

                      case _ =>
                        <h4 class={BoincClientStyle.pageHeaderSmall.htmlClass}>
                          {notice.title}
                        </h4>
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
        {
          messageTable.component
        }
      </div>
    </div>
  }

  private def jsOnContentMountAction(content: String): dom.html.Paragraph => Unit = p => {
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
