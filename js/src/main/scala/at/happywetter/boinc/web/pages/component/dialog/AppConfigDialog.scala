package at.happywetter.boinc.web.pages.component.dialog
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.HTMLElement
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.Elem

import at.happywetter.boinc.shared.boincrpc.AppConfig.AppVersion
import at.happywetter.boinc.shared.boincrpc.{AppConfig, Project}
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style, TableTheme}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.TaskSpecCache
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._

import mhtml.{Rx, Var}

class AppConfigDialog(parentID: String, project: Project)(implicit boinc: BoincClient)
    extends Dialog("appconfig-dialog"):

  NProgress.start()
  private val loading = Var(0)

  private val apps = boinc.getState
    .map(state => {
      loading.update(_ + 1)
      state.apps.filter { case (_, app) => app.project == project.url }
    })
    .toRx(Map.empty)

  private val config = boinc
    .getAppConfig(project.url)
    .map(r => { loading.update(_ + 1); r })
    .toRx(
      AppConfig(List.empty, List.empty, None, reportResultsImmediately = false)
    )

  override def render(): Elem =
    <div class={Style.modal.htmlClass} id="appconfig-dialog" onclick={jsBackgroundAction}>
      <div class={Style.content.htmlClass}  style="max-width:85%">
        <div class={Style.header.htmlClass}>
          <h3>{"app_config_dialog".localize}{": "}{project.name}</h3>
        </div>
        <div class={Style.body.htmlClass} style={s"max-height:${maxHeight}px"}>

          <h4 class={BoincClientStyle.h4.htmlClass}>{"status".localize}</h4>
          <ul>
            <li>{"is_loading".localize}{": "}{
      loading.map(p => { if (p == 2) NProgress.done(true); p != 2 }).map(_.localize)
    }</li>
            <li>{"has_app_config".localize}{": "}{
      config.map(c => c.apps.nonEmpty || c.appVersions.nonEmpty).map(_.localize)
    }</li>
            <li>{"max_concurrent".localize}{": "}{
      config.map(_.projectMaxConcurrent.map(_.toString).getOrElse("not_set".localize))
    }</li>
            <li>{"report_results_immediately"}{": "}{config.map(_.reportResultsImmediately.localize)}</li>
          </ul>

          <h4 class={BoincClientStyle.h4.htmlClass}>{"apps".localize}</h4>
          <div class={TableTheme.container.htmlClass} style="padding-right:70px;padding-top:110px">
          <table class={Seq(TableTheme.table.htmlClass, TableTheme.noBorder.htmlClass).mkString(" ")}>
            <thead>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{
      "user_friendly_name".localize
    }</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"app_name".localize}</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"ngpus".localize}</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"avg_cpus".localize}</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"plan_class".localize}</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"cmdline".localize}</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{
      "report_results_immediately".localize
    }</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"max_concurrent".localize}</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{
      "fraction_done_exact".localize
    }</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"gpu_usage".localize}</span></div></th>
              <th class={
      TableTheme.verticalText.htmlClass
    }><div style="margin-left:calc(100% - 8px);margin-bottom:1px"><span>{"cpu_usage".localize}</span></div></th>
              <th></th>
            </thead>
            <tbody>
              {
      apps.zip(config).map { case (apps, config) =>
        val appConfig = config.apps.map(app => (app.name, app)).toMap
        val appVersion = config.appVersions.map(appV => (appV.appName, appV)).toMap

        println(apps)
        println(appConfig)
        println(appVersion)

        apps.toSeq.sortBy(_._2.name)(StringLengthAlphaOrdering).map { case (_, app) =>
          val appCfg = appConfig.get(app.name)
          val appVer = appVersion.get(app.name)
          val taskSpec =
            TaskSpecCache
              .get(boinc.hostname, app.name)
              .map(
                _.map(app =>
                  AppVersion(app.name, app.version.planClass.getOrElse(""), app.version.avgCpus.ceil.toInt, 0, "")
                )
              )

          val appVerRx: Rx[Option[AppVersion]] =
            if (appVer.isDefined) Var(appVer)
            else taskSpec.toRx(Option.empty)

          <tr>
                      <td>{app.userFriendlyName}</td>
                      <td>{app.name}</td>
                      <td><input style="width:70px" type="number" value={
            appVerRx.map(_.map(_.ngpus.toString).getOrElse(""))
          }/></td>
                      <td><input style="width:70px" type="number" value={
            appVerRx.map(_.map(_.avgCpus.toString).getOrElse(""))
          }/></td>
                      <td><input style="width:70px" type="text" value={
            appVerRx.map(_.map(_.planClass).getOrElse(""))
          }/></td>
                      <td><input style="width:70px" type="text" value={appVerRx.map(_.map(_.cmdline))}/></td>
                      <td><input style="width:70px" type="checkbox" value={
            appCfg.map(_.reportResultsImmediately.localize)
          }/></td>
                      <td><input style="width:70px" type="number" value={appCfg.map(_.maxConcurrent.toString)}/></td>
                      <td><input style="width:70px" type="checkbox" value={
            appCfg.map(_.fractionDoneExact.localize)
          }/></td>
                      <td><input style="width:70px" type="number" value={
            appCfg.map(_.gpuVersions.map(_.gpuUsage.toString).getOrElse("not_set".localize))
          }/></td>
                      <td><input style="width:70px" type="number" value={
            appCfg.map(_.gpuVersions.map(_.gpuUsage.toString).getOrElse("not_set".localize))
          }/></td>
                      <td style="width: 70px;text-align: center;">
                      {
            new Tooltip(
              Var("save".localize),
              <a href="#save-app_config" style="color:#333;text-decoration:none;font-size:30px" onclick={
                jsSaveRowAction
              }>
                            <i class="fas fa-save"></i>
                          </a>
            ).toXML
          }
                      </td>
                    </tr>
        }

      }
    }
            </tbody>
          </table>
          </div>
        </div>
        <div class={Style.footer.htmlClass}>
          <button name="dialog_close_btn" class={Style.button.htmlClass} onclick={
      (event: Event) => {
        event.preventDefault()
        this.hide()
        this.destroy()
      }
    } autofocus="autofocus">
            {"dialog_close".localize}
          </button>
        </div>
      </div>
    </div>

  private val maxHeight = dom.window.innerHeight - 350.0d

  private val jsBackgroundAction: Event => Unit = { event =>
    event.preventDefault()
    if (event.target.asInstanceOf[HTMLElement].id == "appconfig-dialog")
      this.hide()
  }

  private val jsSaveRowAction: Event => Unit = { event =>
    event.preventDefault()
    dom.window.alert("Not implemented!")
  }

  override def show(): Unit =
    super.show()

    if (Dialog.exists(parentID))
      Dialog.hideByID(parentID)

  override def hide(): Unit =
    if (Dialog.exists(parentID))
      Dialog.showByID(parentID)

    super.hide()
    super.destroy()
