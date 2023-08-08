package at.happywetter.boinc.web.pages.swarm

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.HTMLInputElement
import scala.collection.mutable
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dictionary
import scala.xml.{Elem, Node}

import at.happywetter.boinc.shared.boincrpc.BoincRPC.Modes
import at.happywetter.boinc.shared.boincrpc.CCState
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.boincclient.{ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.pages.{BoincSwarmPageStyle => Style}
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.util.XMLHelper._

import mhtml.{Rx, Var}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
object BoincSwarmPage:

  def link = "/view/swarm/boinc"

  sealed private trait ComputingMode
  private object GPU extends ComputingMode
  private object CPU extends ComputingMode
  private object Network extends ComputingMode
  private object RunMode extends ComputingMode

  private object UICCState extends Enumeration:
    val Enabled: UICCState.Value = Value(CCState.State.Enabled.id)
    val Auto: UICCState.Value = Value(CCState.State.Auto.id)
    val Disabled: UICCState.Value = Value(CCState.State.Disabled.id)
    val Loading: UICCState.Value = Value(-1)
    val Error: UICCState.Value = Value(-2)

  private def stateToText(state: UICCState.Value): String = state match
    // case UICCState.Always  => "always".localize
    case UICCState.Auto     => "auto".localize
    case UICCState.Enabled  => "always".localize
    case UICCState.Disabled => "never".localize
    case a                  => a.toString

  implicit private def integerToCCState(value: Int): UICCState.Value = UICCState.apply(value)

  implicit private def convertModetoUIState(mode: Modes.Value): UICCState.Value = mode match
    case Modes.Auto    => UICCState.Auto
    case Modes.Always  => UICCState.Enabled
    case Modes.Never   => UICCState.Disabled
    case Modes.Restore => UICCState.Auto

  implicit private class RichMode(val state: UICCState.Value) extends AnyVal:
    def toState: String = stateToText(state)

class BoincSwarmPage extends SwarmPageLayout:
  import BoincSwarmPage._

  override val header = "Boinc"
  override val path: String = "boinc"

  private case class ClientEntry(run: UICCState.Value, cpu: UICCState.Value, gpu: UICCState.Value, net: UICCState.Value)
  private val clients = Var(Map.empty[String, Var[Option[Either[ClientEntry, Exception]]]])
  private val checkAllState = Var(true)

  override def beforeRender(params: Dictionary[String]): Unit =
    super.beforeRender(params)

    clients := ClientManager.clients.keys.map(name => (name, Var(Option.empty[Either[ClientEntry, Exception]]))).toMap

  override def already(): Unit = onRender()

  override def onRender(): Unit =
    NProgress.start()

    ClientManager.getClients
      .map(
        _.map(boinc =>
          boinc.getCCState
            .map { state =>
              clients.map { clients =>
                val data: Option[Either[ClientEntry, Exception]] = Some(
                  Left(ClientEntry(UICCState.Auto, state.taskMode, state.gpuMode, state.networkMode))
                )

                if (clients.contains(boinc.hostname)) clients(boinc.hostname) := data
                else this.clients.update(v => v.updated(boinc.hostname, Var(data)))
              }.now
            }
            .recover { case ex: Exception =>
              ex.printStackTrace()

              clients.map { clients =>
                if (clients.contains(boinc.hostname)) clients(boinc.hostname) := Some(Right(ex))
                else this.clients.update(v => v.updated(boinc.hostname, Var(Some(Right(ex)))))
              }.now
            }
        )
      )
      .foreach { x =>
        Future.sequence(x).foreach(_ => NProgress.done(true))
      }

  override def renderChildView: Elem =
    <div>
      <table class={TableTheme.table.htmlClass} id="swarm-host-choose-table" style="width:auto">
        <thead>
          <tr class={BoincClientStyle.inTextIcon.htmlClass}>
            <th>
              <a class={Style.masterCheckbox.htmlClass} onclick={jsSelectAllListener}>
                {
      new Tooltip(
        checkAllState.map(status => if (status) "check_all".localize else "uncheck_all".localize),
        <i class={checkAllState.map(state => s"far fa${if (state) "-check" else ""}-square")} href="#select-all"></i>
      ).toXML
    }
              </a>
              <span style="float:left">{"table_host".localize}</span>
            </th>
            <th><i class="fa fa-tasks"></i>{"table_status_cpu".localize}</th>
            <th><i class="fa fa-tv"></i>{"table_status_gpu".localize}</th>
            <th><i class="fa fa-exchange-alt"></i>{"table_status_net".localize}</th>
          </tr>
        </thead>
        <tbody>
          {
      clients.map(
        _.toSeq
          .sortBy(_._1)(ord = StringLengthAlphaOrdering)
          .map(client => {
            <tr>
                  <td><input class={Style.checkbox.htmlClass} type="checkbox" data-client={client._1}></input>{
              injectErrorTooltip(client)
            }</td>
                  <td>{client._2.map(_.map(_.fold(_.run.toState, _ => "")).getOrElse("offline".localize))}</td>
                  <td>{client._2.map(_.map(_.fold(_.gpu.toState, _ => "")).getOrElse("offline".localize))}</td>
                  <td>{client._2.map(_.map(_.fold(_.net.toState, _ => "")).getOrElse("offline".localize))}</td>
                </tr>
          })
          .toSeq
      )
    }
        </tbody>
      </table>

      <h4 class={BoincClientStyle.h4WithoutLine.htmlClass}>{"swarm_boinc_net_state".localize}</h4>
      <table class={TableTheme.table.htmlClass} style="width:auto!important">
        <tbody class={BoincClientStyle.inTextIcon.htmlClass}>
          <tr>
            <td><i class="fa fa-tasks" aria-hidden="true"></i>{"boinc_info_run_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(RunMode, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(RunMode, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(RunMode, Modes.Never)}>{"never".localize}</a></td>
          </tr>
          <tr>
            <td><i class="fa fa-tv" aria-hidden="true"></i>{"boinc_info_gpu_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(GPU, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(GPU, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(GPU, Modes.Never)}>{"never".localize}</a></td>
          </tr>
          <tr>
            <td><i class="fa fa-exchange-alt" aria-hidden="true"></i>{"boinc_info_network_mode".localize}</td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(Network, Modes.Always)}>{"always".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(Network, Modes.Auto)}>{"auto".localize}</a></td>
            <td><a class={Style.button.htmlClass} onclick={jsAction(Network, Modes.Never)}>{"never".localize}</a></td>
          </tr>
        </tbody>
      </table>
    </div>

  // TODO: Code duplication with Dashboard, should move to own utility
  private def injectErrorTooltip(client: (String, Var[Option[Either[ClientEntry, Exception]]])): Rx[Seq[Node]] =
    val data = client._2
    val name = client._1

    data.map { dataOption =>
      dataOption
        .map { data =>
          data.fold(
            _ => Seq(name.toXML),
            ex =>
              Seq(
                ex match {
                  case _: FetchResponseException => Tooltip.warningTriangle("offline").toXML
                  case _                         => Tooltip.warningTriangle("error".localize).toXML
                },
                name.toXML
              )
          )
        }
        .getOrElse(
          Seq(
            <span style="padding-left:1em">
            {Tooltip.loadingSpinner("loading").toXML}
          </span>,
            name.toXML
          )
        )
    }

  private def jsAction(status: ComputingMode, mode: Modes.Value): Event => Unit = event => {
    event.preventDefault()
    NProgress.start()

    import at.happywetter.boinc.web.facade.NodeListConverter.convNodeList
    val boxes = dom.document.querySelectorAll("#swarm-host-choose-table input[type='checkbox']")
    val changeList = new mutable.ArrayBuffer[Future[(String, ComputingMode, Boolean)]](boxes.length)

    boxes.forEach((node, _, _) => {
      val checkBox = node.asInstanceOf[HTMLInputElement]

      if (checkBox.checked) {
        val name = node.asInstanceOf[HTMLInputElement].dataset.apply("client")
        val client = ClientManager.clients(name)

        val action = status match {
          case CPU =>
            clients.map(_(name).update(opt => transformToLoading(opt, _.copy(cpu = UICCState.Loading)))).now;
            client.setCpu(mode)
          case GPU =>
            clients.map(_(name).update(opt => transformToLoading(opt, _.copy(gpu = UICCState.Loading)))).now;
            client.setGpu(mode)
          case Network =>
            clients.map(_(name).update(opt => transformToLoading(opt, _.copy(net = UICCState.Loading)))).now;
            client.setNetwork(mode)
          case RunMode =>
            clients.map(_(name).update(opt => transformToLoading(opt, _.copy(run = UICCState.Loading)))).now;
            client.setRun(mode)
        }

        action.map { ret =>
          status match {
            case CPU if ret     => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(cpu = mode)))).now;
            case GPU if ret     => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(gpu = mode)))).now;
            case Network if ret => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(net = mode)))).now;
            case RunMode if ret => clients.map(_(name).update(opt => transformToLoading(opt, _.copy(run = mode)))).now;
            case _ if !ret =>
              clients.map(_(name).update(opt => transformToLoading(opt, _.copy(cpu = UICCState.Error)))).now
              client.getCCState
                .map { state =>
                  clients.map(
                    _(name) := Some(Left(ClientEntry(UICCState.Auto, state.taskMode, state.gpuMode, state.networkMode)))
                  )
                }
                .recover { case ex: Exception =>
                  clients.map(_(name) := Some(Right(ex)))
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

    import at.happywetter.boinc.web.facade.NodeListConverter.convNodeList
    boxes.forEach((node, _, _) => node.asInstanceOf[HTMLInputElement].checked = checked)

    checkAllState.update(!_)
  }

  private def transformToLoading(entry: Option[Either[ClientEntry, Exception]],
                                 f: ClientEntry => ClientEntry
  ): Option[Either[ClientEntry, Exception]] = entry match
    case None              => None
    case Some(Right(_))    => None
    case Some(Left(entry)) => Some(Left(f(entry)))
