package at.happywetter.boinc.web.pages.component.dialog
import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.boincclient.BoincFormatter
import at.happywetter.boinc.web.css.definitions.components.BasicModalStyle
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.pages.BoincProjectStyle
import at.happywetter.boinc.web.model.ProjectDataTableModel.ReactiveProject
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.StatisticPlatforms
import org.scalajs.dom.Event

object ProjectInfoDialog:
  import at.happywetter.boinc.web.facade.Implicits.JSNumberOps

  def apply(project: ReactiveProject, jsOnDelete: Event => Unit)(implicit boinc: BoincClient): OkDialog =
    new OkDialog(
      "project_dialog_properties".localize + " " + project.data.name,
      List(
        <h4 class={BoincClientStyle.h4.htmlClass}>{"project_dialog_general_header".localize}</h4>,
        <table class={TableTheme.table.htmlClass}>
          <tbody>
            <tr>
              <td><b>{"project_dialog_url".localize}</b></td>
              <td>
                <a class={BoincProjectStyle.link.htmlClass} href={project.data.url} onclick={AppRouter.openExternal}>
                  {project.data.url}
                </a>
              </td>
            </tr>
            <tr><td><b>{"login_username".localize}</b></td><td>{project.data.userName}</td></tr>
            <tr><td><b>{"project_dialog_teamname".localize}</b></td><td>{project.data.teamName}</td></tr>
            <tr><td><b>{"resource_share".localize}</b></td><td>{project.data.resourceShare}</td></tr>
            <tr><td><b>{"disk_usage".localize}</b></td><td>{
          BoincFormatter.convertSize(project.data.desiredDiskUsage)
        }</td></tr>
            <tr><td><b>{"project_dialog_cpid".localize}</b></td><td>{project.data.cpid}</td></tr>
            <tr>
              <td><b>{"project_dialog_host_id".localize}</b></td>
              <td>
                {project.data.hostID}
                <span style="float:right">
                  <a href={StatisticPlatforms.freedc(project.data.cpid)} target="_blank">
                    <img src="/files/images/freedc_icon.png" alt="freedc-icon"></img>
                  </a>
                  <a href={StatisticPlatforms.boincStats(project.data.cpid)} target="_blank">
                    <img src="/files/images/boincstats_icon.png" alt="boincstats-icon"></img>
                  </a>
                </span>
              </td>

            </tr>
            <tr><td><b>{"project_dialog_paused".localize}</b></td><td>{
          project.dontRequestWork.map(_.localize)
        }</td></tr>
            <tr><td><b>{"project_dialog_jobs_succ".localize}</b></td><td>{project.data.jobSucc}</td></tr>
            <tr><td><b>{"project_dialog_jobs_err".localize}</b></td><td>{project.data.jobErrors}</td></tr>
          </tbody>
        </table>,
        <h4 class={BoincClientStyle.h4.htmlClass}>{"project_dialog_credits_header".localize}</h4>,
        <table class={TableTheme.table.htmlClass}>
          <tbody>
            <tr><td><b>{"project_dialog_credits_user".localize}</b></td><td style="text-align:right">{
          project.data.userTotalCredit.asInstanceOf[JSNumberOps].toLocaleString()
        }</td></tr>
            <tr><td><b>{"project_dialog_credits_uavg".localize}</b></td><td style="text-align:right">{
          project.data.userAvgCredit.asInstanceOf[JSNumberOps].toLocaleString()
        }</td></tr>
            <tr><td><b>{"project_dialog_credits_host".localize}</b></td><td style="text-align:right">{
          project.data.hostTotalCredit.asInstanceOf[JSNumberOps].toLocaleString()
        }</td></tr>
            <tr><td><b>{"project_dialog_credits_havg".localize}</b></td><td style="text-align:right">{
          project.data.hostAvgCredit.asInstanceOf[JSNumberOps].toLocaleString()
        }</td></tr>
          </tbody>
        </table>,
        <h4 class={BoincClientStyle.h4.htmlClass}>{"project_actions".localize}</h4>,
        <div>
          <ul class={BasicModalStyle.actionList.htmlClass}>
            <li>
              <a class={BasicModalStyle.action.htmlClass} href="#deleteProject"
                 onclick={jsOnDelete}>
                {"project_delete".localize}
              </a>
            </li>
            <li>
              <a class={BasicModalStyle.action.htmlClass} href="#appConfig" onclick={
          (event: Event) => {
            new AppConfigDialog("modal-dialog-type1", project.data).renderToBody().show();
          }
        }>
                {"edit_app_config".localize}
              </a>
            </li>
          </ul>
        </div>
      )
    )
