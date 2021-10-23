package at.happywetter.boinc.web.pages.component.dialog
import at.happywetter.boinc.shared.boincrpc.BoincRPC
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.rpc.jobs.{At, BoincProjectAction, BoincRunModeAction, CPU, Every, GPU, Job, JobAction, JobMode, Network, Once, Running}
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.JobManagerClient
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.components.{TableTheme, BasicModalStyle => Style}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N.TranslatableString
import at.happywetter.boinc.web.util.RichRx.NowRx
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.{Event, document}
import org.scalajs.dom.raw.{HTMLInputElement, HTMLSelectElement}

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date
import scala.xml.{Elem, Text}


class JobAddDialog(onComplete: Option[Job] => Unit) extends Dialog("job-modal-dialog") {

  private val jobSchedule = Var(Option.empty[String])
  private val jobAction = Var(Option.empty[String])
  private val projects = Var(List.empty[String])

  ClientManager
    .queryCompleteProjectList()
    .foreach(projects => {
      this.projects := projects.keys.toList
      NProgress.done(true)
    })

  override def render(): Elem =
    <div class={Style.modal.htmlClass} id="job-modal-dialog">
      <div class={Style.content.htmlClass}  style="max-width:85%">
        <div class={Style.header.htmlClass}>
          <h3>{"job_add_dialog".localize}</h3>
        </div>
        <div class={Style.body.htmlClass}>
          <labe><b>{"name".localize}:</b><input id="job_name" name="job_name" type="text" style="margin-left:8px;min-width:170px"/></labe>
          <br/>
          <br/>
          <br/>

          <div style="border-bottom:1px solid #DDD">
            <h4 class={BoincClientStyle.h4WithoutLine.htmlClass} style="display:inline-block">{"job_schedule".localize}</h4>
            <select onchange={jsOnJobScheduleSelect}>
              <option disabled={true} selected={true}>{"job_select_schedule".localize}</option>
              <option value="once">{"job_schedule_once".localize}</option>
              <option value="every">{"job_schedule_every".localize}</option>
              <option value="at">{"job_schedule_at".localize}</option>
            </select>
          </div>
          <br/>
          {
            jobSchedule.map {
              case Some("once")  =>
                <p>{"job_description_once".localize}</p>

              case Some("every") =>
                <div>
                  <p>{"job_description_every".localize}</p>
                  <table class={TableTheme.table.htmlClass}>
                    <tbody>
                      <tr>
                        <td><b><label for="time_input">{"interval".localize}:</label></b></td>
                        <td><input id="every_interval_input" name="time_input" type="time"/></td>
                      </tr>
                      <tr>
                        <td><b><label for="end_date">{"end_date".localize}:</label></b></td>
                        <td><input id="every_until_input" name="end_date" type="datetime-local"/></td>
                      </tr>
                    </tbody>
                  </table>
                </div>

              case Some("at") =>
                <div>
                  <p>{"job_description_at".localize}</p>
                  <table class={TableTheme.table.htmlClass}>
                    <tbody>
                      <tr>
                        <td><b><label for="at_input">{"at".localize}:</label></b></td>
                        <td><input id="at_timestamp_input" name="at_input" type="datetime-local"/></td>
                      </tr>
                    </tbody>
                  </table>
                </div>

              case None =>
                Text("")

            }
          }

          <br/>
          <div style="border-bottom:1px solid #DDD">
            <h4 class={BoincClientStyle.h4WithoutLine.htmlClass} style="display:inline-block">{"job_action".localize}</h4>
            <select onchange={jsOnJobActionSelect}>
              <option disabled={true} selected={true}>{"job_select_action".localize}</option>
              <option value="boinc_project">{"job_action_boinc_project".localize}</option>
              <option value="boinc_runmode">{"job_action_boinc_runmode".localize}</option>
            </select>
          </div>
          <br/>
          {
            jobAction.map {
              case None => Text("")
              case Some("boinc_project") =>
                <div>
                  <table class={TableTheme.table.htmlClass}>
                    <tbody>
                      <tr>
                        <td><b><label for="project_select">{"project".localize}:</label></b></td>
                        <td>
                          <select id="job_action_url" name="project_select">
                            <option disabled={true} selected={true}>{"select_project".localize}</option>
                            {
                            projects.map(projects => projects.map(project =>
                              <option value={project}>{project}</option>
                            ))
                            }
                          </select>
                        </td>
                      </tr>
                      <tr>
                        <td><b><label for="action_select">{"action".localize}:</label></b></td>
                        <td>
                          <select id="job_action" name="action_select">
                            <option disabled={true} selected={true}>{"select_project_action".localize}</option>
                            {
                              ProjectAction.values.map(_.toString).toSeq.sorted.map(action =>
                                <option value={action}>{action.localize}</option>
                              )
                            }
                          </select>
                        </td>
                      </tr>
                      <tr>
                        <td><b>{"hosts".localize}</b></td>
                        <td>{ hostSelection }</td>
                      </tr>
                    </tbody>
                  </table>
                </div>

              case Some("boinc_runmode") =>
                <div>
                  <table class={TableTheme.table.htmlClass}>
                    <tbody>
                      <tr>
                        <td><b><label for="action_target">{"target".localize}:</label></b></td>
                        <td>
                          <select id="job_action_target" name="action_target">
                            <option disabled={true} selected={true}>{"select_target".localize}</option>
                            <option value="cpu">{"cpu".localize}</option>
                            <option value="gpu">{"gpu".localize}</option>
                            <option value="net">{"network".localize}</option>
                          </select>
                        </td>
                      </tr>
                      <tr>
                        <td><b><label for="action_mode">{"mode".localize}:</label></b></td>
                        <td>
                          <select id="job_action_mode" name="action_mode">
                            <option disabled={true} selected={true}>{"select_mode".localize}</option>
                            {
                              BoincRPC.Modes.values.map(_.toString).toSeq.sorted.map(mode =>
                                <option value={mode}>{mode.localize}</option>
                              )
                            }
                          </select>
                        </td>
                      </tr>
                      <tr>
                        <td><b><label for="duration">{"duration".localize}:</label></b></td>
                        <td><input id="job_action_duration" name="duration" type="number"/></td>
                      </tr>
                      <tr>
                        <td><b>{"hosts".localize}</b></td>
                        <td>{ hostSelection }</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
            }
          }

        </div>
        <br/>
        <div class={Style.footer.htmlClass}>
          <button name="dialog_save_btn" style="background-color:#42734B" class={Style.button.htmlClass} onclick={jsOnSubmit} autofocus="autofocus">
            {"dialog_save".localize}
          </button>
          <button name="dialog_close_btn" class={Style.button.htmlClass} onclick={(event: Event) => {
            event.preventDefault()
            this.hide()
            this.destroy()
          }}>
            {"dialog_close".localize}
          </button>
        </div>
      </div>
    </div>


  private def hostSelection =
    <select multiple={true} id="hosts">
      {
        ClientManager.clients.keys.toSeq.sorted(ord = StringLengthAlphaOrdering).map(host =>
          <option value={host}>{host}</option>
        )
      }
    </select>

  private val jsOnJobScheduleSelect: Event => Unit = event => {
    jobSchedule := Some(event.target.asInstanceOf[HTMLSelectElement].value)
  }

  private val jsOnJobActionSelect: Event => Unit = event => {
    jobAction := Some(event.target.asInstanceOf[HTMLSelectElement].value)
  }

  private val jsOnSubmit: Event => Unit = event => {
    event.preventDefault()
    val name = document.querySelector("#job_name").asInstanceOf[HTMLInputElement].value

    NProgress.start()
    JobManagerClient.create(
      Job(None, name, buildSchedule(), buildAction(), Running)
    ).map(Some.apply)
     .recover(_ => Option.empty)
     .map { x =>
       NProgress.done(true)
       this.hide()
       x
     }
     .foreach(onComplete)
  }

  private def buildAction(): JobAction = {
    val action = jobAction.now
    if (action.isEmpty)
      throw new RuntimeException("No Action was chosen!")

    val hosts = document
      .querySelector("#hosts")
      .asInstanceOf[HTMLSelectElement]
      .options
      .filter(_.selected)
      .map(_.value)
      .toList

    action.get match {
      case "boinc_project" =>
        val url = document.querySelector("#job_action_url").asInstanceOf[HTMLInputElement].value
        val action = document.querySelector("#job_action").asInstanceOf[HTMLSelectElement].value
        BoincProjectAction(hosts, url, ProjectAction.fromValue(action).get)

      case "boinc_runmode" =>
        val target = document.querySelector("#job_action_target").asInstanceOf[HTMLSelectElement].value match {
          case "cpu" => CPU
          case "gpu" => GPU
          case "net" => Network
        }

        val mode = document.querySelector("#job_action_mode").asInstanceOf[HTMLSelectElement].value

        BoincRunModeAction(hosts, target, BoincRPC.Modes.fromValue(mode).get)
    }

  }

  private def buildSchedule(): JobMode = {
    jobSchedule.now match {
      case Some("once")  => Once
      case Some("at")    =>
        val timestamp = document.querySelector("#at_timestamp_input").asInstanceOf[HTMLSelectElement].value
        At(LocalDateTime.parse(timestamp))

      case Some("every") =>
        val interval = document.querySelector("#every_interval_input").asInstanceOf[HTMLSelectElement].value
        val until = document.querySelector("#every_until_input").asInstanceOf[HTMLSelectElement].value

        val intervalDuration = {
          val tmp = interval.split(":")
          FiniteDuration(tmp(0).toInt * 60 + tmp(1).toInt, TimeUnit.MINUTES)
        }

        Every(intervalDuration, if (until.isEmpty) Option.empty else Some(LocalDateTime.parse(until)))
    }
  }

}
