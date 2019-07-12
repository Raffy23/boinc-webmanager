package at.happywetter.boinc.web.pages.swarm

import at.happywetter.boinc.shared.boincrpc.BoincRPC.Modes
import at.happywetter.boinc.shared.boincrpc.CCState
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.component.Tooltip
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

import scala.collection.mutable
import scala.scalajs.js.Dictionary
import at.happywetter.boinc.shared.boincrpc.CCState.State
import at.happywetter.boinc.shared.boincrpc.CCState.State.Value

import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
class BoincSwarmPage extends SwarmPageLayout {
  import BoincSwarmPage._

  override val header = "Boinc"
  override val path: String = "boinc"

  private case class ClientEntry(run: UICCState.Value, cpu: UICCState.Value, gpu: UICCState.Value, net: UICCState.Value)
  private val clients = Var(Map.empty[String, Var[Option[Either[ClientEntry, Exception]]]])
  private val checkAllState = Var(true)

  override def beforeRender(params: Dictionary[String]): Unit = {
    super.beforeRender(params)
    clients := ClientManager.clients.keys.map(name => (name, Var(None.asInstanceOf[Option[Either[ClientEntry, Exception]]]))).toMap
    println(clients)
  }

  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    NProgress.start()

    ClientManager
      .getClients
      .map(_.map(boinc =>
        boinc
          .getCCState
          .map { state =>
            clients.map { clients =>
              val data: Option[Either[ClientEntry, Exception]] = Some(Left(ClientEntry(UICCState.Auto, state.taskMode, state.gpuMode, state.networkMode)))
              println(data)

              if (clients.contains(boinc.hostname)) clients(boinc.hostname) := data
              else this.clients.update(v => v.updated(boinc.hostname, Var(data)))
            }.now
          }.recover {
            case ex: Exception =>
              ex.printStackTrace()

              clients.map { clients =>
                if (clients.contains(boinc.hostname)) clients(boinc.hostname) := Some(Right(ex))
                else this.clients.update(v => v.updated(boinc.hostname, Var(Some(Right(ex)))))
              }.now
          }
      )
    ).foreach { x =>
      Future.sequence(x).foreach(x => NProgress.done(true))
    }
  }

  override def renderChildView: Elem = {
    <div>
      <table class={TableTheme.table.htmlClass} id="swarm-host-choose-table" style="width:auto">
        <thead>
          <tr class={BoincClientLayout.Style.in_text_icon.htmlClass}>
            <th>
              <a class={Style.masterCheckbox.htmlClass} onclick={jsSelectAllListener}>
                <i class={checkAllState.map( state => s"fa fa${ if (state) "-check" else ""}-square-o")} href="#select-all"></i>
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
            clients.map(_.map(client => {
                <tr>
                  <td><input class={Style.checkbox.htmlClass} type="checkbox" data-client={client._1}></input>{injectErrorTooltip(client)}</td>
                  <td>{client._2.map(_.map(_.fold(_.run.toState, _ => "")).getOrElse("offline".localize))}</td>
                  <td>{client._2.map(_.map(_.fold(_.gpu.toState, _ => "")).getOrElse("offline".localize))}</td>
                  <td>{client._2.map(_.map(_.fold(_.net.toState, _ => "")).getOrElse("offline".localize))}</td>
                </tr>
            }).toSeq)
          }
        </tbody>
      </table>

      <h4 class={BoincClientLayout.Style.h4_without_line.htmlClass}>{"swarm_boinc_net_state".localize}</h4>
      <table class={TableTheme.table.htmlClass} style="width:auto!important">
        <tbody class={BoincClientLayout.Style.in_text_icon.htmlClass}>
          <tr>
            <td><i class="fa fa-tasks" aria-hidden="true"></i>{"boinc_info_run_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(RunMode, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(RunMode, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(RunMode, Modes.Never)}>{"never".localize}</a></td>
          </tr>
          <tr>
            <td><i class="fa fa-television" aria-hidden="true"></i>{"boinc_info_gpu_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(GPU, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(GPU, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(GPU, Modes.Never)}>{"never".localize}</a></td>
          </tr>
          <tr>
            <td><i class="fa fa-exchange" aria-hidden="true"></i>{"boinc_info_network_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(Network, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(Network, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(Network, Modes.Never)}>{"never".localize}</a></td>
          </tr>
        </tbody>
      </table>
    </div>
  }

  // TODO: Code duplication with Dashboard, should move to own utility
  private def injectErrorTooltip(client: (String, Var[Option[Either[ClientEntry, Exception]]])): Rx[Seq[Node]] = {
    def buildTooltip(label: String, `class`: String = "fa fa-exclamation-triangle", color: String = "#FF8181"): Node = {
      val tooltip = new Tooltip(
        Var(label.localize),
        <i class={`class`} aria-hidden="true"></i>
      ).toXML.asInstanceOf[Elem]

      tooltip.copy(
        attributes1 = UnprefixedAttribute("style", s"float:right;color:$color", tooltip.attributes1)
      )
    }

    val data = client._2
    val name = client._1

    data.map { dataOption =>
      dataOption.map { data =>
        data.fold(
          _ => Seq(name.toXML),
          ex =>
            Seq(
              ex match {
                case _: FetchResponseException => buildTooltip("offline")
                case _ => buildTooltip("error".localize)
              },
              name.toXML
            )
        )
      }.getOrElse(
        Seq(
          <span style="padding-left:1em">{buildTooltip("loading", "fa fa-spinner fa-pulse", "#428bca")}</span>,
          name.toXML
        )
      )
    }
  }

  private def jsAction(status: ComputingMode, mode: Modes.Value): Event => Unit = event => {
    event.preventDefault()
    NProgress.start()

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    val boxes = dom.document.querySelectorAll("#swarm-host-choose-table input[type='checkbox']")
    val changeList = new mutable.ArrayBuffer[Future[(String, ComputingMode, Boolean)]](boxes.length)

    boxes.forEach( (node, _, _) => {
      val checkBox = node.asInstanceOf[HTMLInputElement]

      if (checkBox.checked) {
        val name = node.asInstanceOf[HTMLInputElement].dataset("client")
        val client = ClientManager.clients(name)

        val action = status match {
          case CPU     => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(cpu = UICCState.Loading)))).now; client.setCpu(mode)
          case GPU     => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(gpu = UICCState.Loading)))).now; client.setGpu(mode)
          case Network => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(net = UICCState.Loading)))).now; client.setNetwork(mode)
          case RunMode => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(run = UICCState.Loading)))).now; client.setRun(mode)
        }

        action.map { ret =>
          status match {
            case CPU     if  ret => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(cpu = mode)))).now;
            case GPU     if  ret => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(gpu = mode)))).now;
            case Network if  ret => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(net = mode)))).now;
            case RunMode if  ret => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(run = mode)))).now;
            case _       if !ret =>
              clients.map(_(name).update(opt => transformToLoading(opt, _.copy(cpu = UICCState.Error)))).now
              client.getCCState
                .map { state =>
                  clients.map(_(name) := Some(Left(ClientEntry(UICCState.Auto, state.taskMode, state.gpuMode, state.networkMode))))
                }.recover{
                  case ex: Exception => clients.map(_(name) := Some(Right(ex)))
                }
          }

          (name, status, ret)
        }

      }
    })

    Future.sequence(changeList).foreach(_ => NProgress.done(true))
  }

  private val jsSelectAllListener: Event => Unit = event => {
    event.preventDefault()
    val boxes = dom.document.querySelectorAll("#swarm-host-choose-table input[type='checkbox']")
    val checked = checkAllState.now

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    boxes.forEach((node, _, _) => node.asInstanceOf[HTMLInputElement].checked = checked)

    checkAllState.update(!_)
  }

  private def transformToLoading(entry: Option[Either[ClientEntry, Exception]], f: ClientEntry => ClientEntry): Option[Either[ClientEntry, Exception]] = entry match {
      case None              => None
      case Some(Left(entry)) => println(f(entry)); Some(Left(f(entry)))
      case Some(Right(_))    => None
  }

}

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

  def link = "/view/swarm/boinc"

  private sealed trait ComputingMode
  private object GPU extends ComputingMode
  private object CPU extends ComputingMode
  private object Network extends ComputingMode
  private object RunMode extends ComputingMode

  private object UICCState extends Enumeration {
    val Enabled: UICCState.Value  = Value(CCState.State.Enabled.id)
    val Auto: UICCState.Value     = Value(CCState.State.Auto.id)
    val Disabled: UICCState.Value = Value(CCState.State.Disabled.id)
    val Loading: UICCState.Value  = Value(-1)
    val Error: UICCState.Value    = Value(-2)
  }

  private def stateToText(state: UICCState.Value): String = state match {
    //case UICCState.Always  => "always".localize
    case UICCState.Auto     => "auto".localize
    case UICCState.Enabled  => "always".localize
    case UICCState.Disabled => "never".localize
    case a => a.toString
  }

  private implicit def integerToCCState(value: Int): UICCState.Value = UICCState.apply(value)

  private implicit def convertModetoUIState(mode: Modes.Value): UICCState.Value = mode match {
    case Modes.Auto => UICCState.Auto
    case Modes.Always => UICCState.Enabled
    case Modes.Never => UICCState.Disabled
    case Modes.Restore => UICCState.Auto
  }

  private implicit class RichMode(val state: UICCState.Value) extends AnyVal {
    def toState: String = stateToText(state)
  }

}
