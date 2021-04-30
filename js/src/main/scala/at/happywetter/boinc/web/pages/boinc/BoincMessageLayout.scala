package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.{Message, Notice}
import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.boincclient.BoincFormatter.Implicits._
import at.happywetter.boinc.web.css.CSSIdentifier
import at.happywetter.boinc.web.css.definitions.components.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle, BoincMessageStyle => Style}
import at.happywetter.boinc.web.model.MessageTableModel.MessageTableRow
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.routes.{AppRouter, NProgress, Navigo}
import at.happywetter.boinc.web.storage.MessageCache
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import mhtml.{Cancelable, Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{DOMParser, HTMLElement, HTMLInputElement, HTMLSelectElement}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.xml.Elem
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.model.DataModelConverter._

import scala.concurrent.Future
import scala.scalajs.js.Dictionary

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
object BoincMessageLayout {

  val SELECTION_NOTICES = "notices"
  val SELECTION_MESSAGES = "messages"

  val DEFAULT_SELECTION: String = SELECTION_NOTICES

}
class BoincMessageLayout extends BoincClientLayout {
  import BoincMessageLayout._

  override val path: String = "messages"

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

  private var selection = Var(DEFAULT_SELECTION)
  private val filter: Var[(Int, Array[String])] = Var((0, Array.empty))
  private var filterCancelable: Cancelable = _


  override def leave(): Unit = {
    filterCancelable.cancel
  }

  override def already(): Unit = {
    filterCancelable.cancel
    onRender()
  }

  override def onRender(): Unit = {
    NProgress.start()

    MessageCache.getLastSeqNo(boincClientName).map { lastSavedSeqNo =>
      boinc
        .getMessages(lastSavedSeqNo)
        .flatMap(messages => MessageCache.save(boincClientName, messages))
        .map(_ => updateMessageTable(filter.now._1, filter.now._2))
        .flatMap { _ =>
          boinc
            .getNotices()
            .map(notice => notices := notice)
        }
        .recover(ErrorDialogUtil.showDialog)
        .map(_ => NProgress.done(true))

    }
      .recover(ErrorDialogUtil.showDialog)
      .map(_ => NProgress.done(true))

    filterCancelable = filter.impure.run{ case (priority, projects) =>
      updateMessageTable(priority, projects)
    }
  }

  private def updateMessageTable(priority: Int, projects: Array[String]): Unit = {
    MessageCache
      .get(boincClientName)
      .map(messageTable.reactiveData := _.toList.filter { message =>
        message.priority >= priority && (projects.isEmpty || projects.exists(message.project.contains))
      }).foreach { _ =>
        messageTable.currentPage :=
          (messageTable.reactiveData.now.length / messageTable.curPageSize.now) + 1
      }
  }

  override def render: Elem = {
    <div id="messages">
      <div class={FloatingMenu.root.htmlClass}>
        <a class={selection.map(s => if(s == SELECTION_NOTICES) Some(FloatingMenu.active.htmlClass) else None)}
           onclick={jsOnSelectionChangeAction(SELECTION_NOTICES)}>
          {"noties_menu_entry".localize}
        </a>
        <a class={selection.map(s => if(s == SELECTION_MESSAGES) Some(FloatingMenu.active.htmlClass) else None)}
           onclick={jsOnSelectionChangeAction(SELECTION_MESSAGES)}>
          {"message_menu_entry".localize}
        </a>
      </div>

      <h3 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-envelope" aria-hidden="true"></i>
        {"messages_header".localize}
      </h3>
      <div id="client-notices" style={selection.map(s => if (s == SELECTION_NOTICES) None else Some("display:none"))}>
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
      <div id="client-messages" style={selection.map(s => if(s == SELECTION_MESSAGES) None else Some("display:none"))}>
        <div class={Style.filterBox.htmlClass}>
          <b>{"filter_by".localize}:</b>
          <label for="filter_priority">{"priority".localize}:</label>
          <select id="filter_priority" onchange={jsOnMessageFilterChangeAction}>
            <option value="0" selected={if (filter.now._1 == 0) Some("selected") else None}>{"all".localize}</option>
            <option value="1" selected={if (filter.now._1 == 1) Some("selected") else None}>{"info".localize}</option>
            <option value="2" selected={if (filter.now._1 == 2) Some("selected") else None}>{"user_alert".localize}</option>
            <option value="3" selected={if (filter.now._1 == 3) Some("selected") else None}>{"internal_error".localize}</option>
            <option value="4" selected={if (filter.now._1 == 4) Some("selected") else None}>{"scheduler_alert".localize}</option>
          </select>
          <label for="filter_project">{"project".localize}:</label>
          <input type="text" id="filter_project" oninput={jsOnProjectFilterChangeAction}></input>
        </div>
        {
          messageTable.component
        }
      </div>
    </div>
  }

  private def jsOnSelectionChangeAction(newSelection: String): Event => Unit = event => {
    import at.happywetter.boinc.web.facade.NodeListConverter._

    event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
      if (!js.isUndefined(node.asInstanceOf[HTMLElement].classList))
        node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
    })
    event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

    selection := newSelection

  }

  private val jsOnProjectFilterChangeAction: Event => Unit = event => {
    val value = event.target.asInstanceOf[HTMLInputElement].value.split(" ")
    filter.update { case (priority, _) => (priority, value) }
  }

  private val jsOnMessageFilterChangeAction: Event => Unit = event => {
    val value = event.target.asInstanceOf[HTMLSelectElement].value.toInt
    filter.update { case (_, projects) => (value, projects) }
  }

  private def jsOnContentMountAction(content: String): dom.html.Paragraph => Unit = p => {
    p.appendChild(convertContent(content))
  }

  private def convertContent(content: String) = {
    import at.happywetter.boinc.web.facade.NodeListConverter._

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
