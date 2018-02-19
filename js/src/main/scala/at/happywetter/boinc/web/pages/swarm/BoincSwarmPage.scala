package at.happywetter.boinc.web.pages.swarm

import at.happywetter.boinc.shared.BoincRPC
import at.happywetter.boinc.shared.BoincRPC.Modes
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.swarm.BoincSwarmPage.Style
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.{Elem, Node, UnprefixedAttribute}
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.helper.XMLHelper._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
object BoincSwarmPage {

  object Style extends StyleSheet.Inline {

    import dsl._

    import scala.language.postfixOps

    val checkbox = style(
      marginRight(10 px)
    )

    val masterCheckbox = style(
      textDecoration := "none",
      color(c"#333"),
      float.left,
      marginLeft(5 px)
    )

    val center = style(
      textAlign.center.important
    )

    val button = style(
      outline.`0`,
      backgroundColor(c"#428bca"),
      border.`0`,
      padding(10 px),
      color(c"#FFFFFF").important,
      cursor.pointer,
      margin(6 px, 6 px),

      &.hover(
        backgroundColor(c"#74a9d8")
      )
    )
  }

}

class BoincSwarmPage extends SwarmPageLayout {

  override val header = "Boinc"

  override val path: String = "boinc"

  private case class ClientEntry(cpu: Int, gpu: Int, net: Int)
  private val clients = Var(List.empty[(String, Future[ClientEntry])])
  private var clientStatus = Map.empty[String, (Var[Int], Var[Int], Var[Int])]


  override def already(): Unit = onRender()

  override def onRender(): Unit = {
      ClientManager
      .getClients
      .map(_.map(boinc =>
        (boinc.hostname,
          boinc
            .getCCState
            .map(state => ClientEntry(state.taskMode, state.gpuMode, state.networkMode)))
        )
      ).foreach(data => {
        clientStatus =
        data.map { case (name, result) =>
          (name, (result.map(_.cpu).toRx(-1), result.map(_.gpu).toRx(-1), result.map(_.net).toRx(-1)))
        }.toMap

        clients := data
      })
  }

  override def renderChildView: Elem = {
    <div>
      <table class={TableTheme.table.htmlClass} id="swarm-host-choose-table" style="width:auto">
        <thead>
          <tr class={BoincClientLayout.Style.in_text_icon.htmlClass}>
            <th>
              <a class={Style.masterCheckbox.htmlClass} onclick={jsSelectAllListener}>
                <i class="fa fa-check-square-o" href="#select-all"></i>
                {"table_host".localize}
              </a>
            </th>
            <th><i class="fa fa-tasks"></i>{"table_status_cpu".localize}</th>
            <th><i class="fa fa-television"></i>{"table_status_gpu".localize}</th>
            <th><i class="fa fa-exchange"></i>{"table_status_net".localize}</th>
          </tr>
        </thead>
        <tbody>
          {
            clients.map(_.map(implicit client => {
                <tr>
                  <td><input class={Style.checkbox.htmlClass} type="checkbox"></input>{injectErrorTooltip(client._1)}</td>
                  <td>{clientStatus(client._1)._1.map(_.toState)}</td>
                  <td>{clientStatus(client._1)._2.map(_.toState)}</td>
                  <td>{clientStatus(client._1)._3.map(_.toState)}</td>
                </tr>
            }))
          }
        </tbody>
      </table>

      <h4 class={BoincClientLayout.Style.h4_without_line.htmlClass}>{"swarm_boinc_net_state".localize}</h4>
      <table class={TableTheme.table.htmlClass} style="width:auto!important">
        <tbody class={BoincClientLayout.Style.in_text_icon.htmlClass}>
          <tr>
            <td><i class="fa fa-tasks"></i>{"boinc_info_run_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setRun, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setRun, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setRun, Modes.Never)}>{"never".localize}</a></td>
          </tr>
          <tr>
            <td><i class="fa fa-television"></i>{"boinc_info_gpu_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setGpu, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setGpu, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setGpu, Modes.Never)}>{"never".localize}</a></td>
          </tr>
          <tr>
            <td><i class="fa fa-exchange"></i>{"boinc_info_network_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setNetwork, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setNetwork, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(_.setNetwork, Modes.Never)}>{"never".localize}</a></td>
          </tr>
        </tbody>
      </table>
    </div>
  }

  private def injectErrorTooltip(name: String)(implicit client: (String, Future[ClientEntry])): Rx[Seq[Node]] = {
    def buildTooltip(label: String, `class`: String = "fa fa-exclamation-triangle"): Node = {
      val tooltip = new Tooltip(
        Var(label.localize),
        <i class={`class`}></i>
      ).toXML.asInstanceOf[Elem]

      tooltip.copy(
        attributes1 = UnprefixedAttribute("style", "float:right;color:#FF8181", tooltip.attributes1)
      )
    }

    client._2
      .map(_ => Seq(name.toXML))
      .recover{ case _: Exception => Seq(name.toXML, buildTooltip(name)) }
      .toRx(Seq(name.toXML)) //TODO: Maybe add Loading icon?
  }

  private def jsAction(func: (BoincClient) => (Modes.Value, Double) => Future[Boolean], mode: Modes.Value): (Event) => Unit = (event) => {
    event.preventDefault()
    NProgress.start()

    Future.sequence(
      ClientManager.clients.values.map(client => {
        func(client)(mode, 0)
          .map(ret => (client.hostname, ret))
          .recover { case _: Exception => (client.hostname, false) }
      })
    ).foreach(results => {
      results.foreach{ case (name, ret) if !ret =>
        ClientManager.clients(name).getCCState
          .foreach(state => {
            clientStatus(name)._1 := state.taskMode
            clientStatus(name)._2 := state.gpuMode
            clientStatus(name)._3 := state.networkMode
          }) //TODO: Error stuff!
      }

      NProgress.done(true)
    })
  }

  private val jsSelectAllListener: (Event) => Unit = (event) => {
    event.preventDefault()
    val boxes = dom.document.querySelectorAll("#swarm-host-choose-table input[type='checkbox']")

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    boxes.forEach((node, _, _) => node.asInstanceOf[HTMLInputElement].checked = true)
  }

  private def stateToText(state: Int): String = state match {
    case -1 => "offline".localize
    case 1 => "always".localize
    case 2 => "auto".localize
    case 3 => "never".localize
    case 4 => "restore".localize
    case a => a.toString
  }

  private implicit class StateInteger(state: Int) {
    def toState: String = stateToText(state)
  }

}
