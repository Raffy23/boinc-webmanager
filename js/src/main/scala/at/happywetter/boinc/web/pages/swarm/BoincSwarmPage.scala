package at.happywetter.boinc.web.pages.swarm

import at.happywetter.boinc.shared.BoincRPC
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
object BoincSwarmPage extends SwarmSubPage {

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


  override def header = "Boinc"

  override def render: JsDom.TypedTag[HTMLElement] ={
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    val root = div(id := "swarm-boinc-content")

    ClientManager.readClients().map(clients => {
      dom.document.getElementById("swarm-boinc-content").appendChild(
       table(TableTheme.table, id := "swarm-host-choose-table", style := "width:auto",
         thead(
           tr(BoincClientLayout.Style.in_text_icon,
             th(a(Style.masterCheckbox,
               i(`class` := "fa fa-check-square-o"), href := "#select-all", onclick := selectAllListener),
               "table_host".localize),
             th(i(`class` := "fa fa-tasks"), "table_status_cpu".localize),
             th(i(`class` := "fa fa-television"), "table_status_gpu".localize),
             th(i(`class` := "fa fa-exchange"), "table_status_net".localize)
           )
         ),
         tbody(
          clients.map(client => {
            val boinc = new BoincClient(client)
            boinc.getCCState.map(state => {
              dom.document.getElementById(client + "-cpu").textContent = stateToText(state.taskMode)
              dom.document.getElementById(client + "-gpu").textContent = stateToText(state.gpuMode)
              dom.document.getElementById(client + "-net").textContent = stateToText(state.networkMode)
            }).recover {
              case _: FetchResponseException =>
                val hField = dom.document.getElementById(client).asInstanceOf[HTMLElement]
                val tooltip = new Tooltip("Offline", i(`class` := "fa fa-exclamation-triangle")).render()
                tooltip.style = "float:right;color:#FF8181"

                hField.appendChild(tooltip)
            }

            tr(
              td(id := client, input(Style.checkbox, `type` := "checkbox"), client),
              td(id := client + "-cpu", Style.center),
              td(id := client + "-gpu", Style.center),
              td(id := client + "-net", Style.center)
            )
          })
         )
       ).render
      )

      renderControlTable()
    })

    root
  }

  private def reRender(): Unit = {
    dom.document.getElementById("swarm-boinc-content").innerHTML = ""
    dom.document.getElementById("swarm-boinc-content").appendChild(render.render.firstChild)
  }

  private def renderControlTable(): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    dom.document.getElementById("swarm-boinc-content").appendChild(
      div(
        h4(BoincClientLayout.Style.h4_without_line, "swarm_boinc_net_state".localize),
        table(TableTheme.table, style := "width:auto!important",
          tbody(BoincClientLayout.Style.in_text_icon,
            tr(
              td(i(`class` := "fa fa-tasks"), "boinc_info_run_mode".localize),
              td(a(Style.button, "always".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setRun(BoincRPC.Modes.Always).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }})),
              td(a(Style.button, "auto".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setRun(BoincRPC.Modes.Auto).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }})),
              td(a(Style.button, "never".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setRun(BoincRPC.Modes.Never).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }}))
            ),
            tr(td(i(`class` := "fa fa-television"), "boinc_info_gpu_mode".localize),
              td(a(Style.button, "always".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setGpu(BoincRPC.Modes.Always).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }})),
              td(a(Style.button, "auto".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setGpu(BoincRPC.Modes.Auto).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }})),
              td(a(Style.button, "never".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setGpu(BoincRPC.Modes.Never).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }}))
            ),
            tr(td(i(`class` := "fa fa-exchange"), "boinc_info_network_mode".localize),
              td(a(Style.button, "always".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setNetwork(BoincRPC.Modes.Always).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }})),
              td(a(Style.button, "auto".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setNetwork(BoincRPC.Modes.Auto).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }})),
              td(a(Style.button, "never".localize, onclick := { (event: Event) => {
                event.preventDefault()
                NProgress.start()

                Future.sequence(
                  ClientManager.clients.values.map(client => {
                    client.setNetwork(BoincRPC.Modes.Never).recover { case _: Exception => false }
                  })
                ).foreach(_ => {
                  NProgress.done(true)
                  reRender()
                })
              }}))
            )
          )
        )
      ).render
    )
  }

  private val selectAllListener: js.Function1[Event, Unit] = (event) => {
    event.preventDefault()
    val boxes = dom.document.querySelectorAll("#swarm-host-choose-table input[type='checkbox']")

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    boxes.forEach((node, _, _) => node.asInstanceOf[HTMLInputElement].checked = true)
  }

  private def stateToText(state: Int): String = state match {
    case 1 => "always".localize
    case 2 => "auto".localize
    case 3 => "never".localize
    case 4 => "restore".localize
    case a => a.toString
  }
}
