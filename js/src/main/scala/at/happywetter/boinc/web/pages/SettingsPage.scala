package at.happywetter.boinc.web.pages

import at.happywetter.boinc.BuildInfo
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.component.topnav.SettingsTopNavigation
import at.happywetter.boinc.web.pages.component.{DashboardMenu, LanguageChooser}
import at.happywetter.boinc.web.routes.{AppRouter, LayoutManager, NProgress}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{AuthClient, DashboardMenuBuilder, ErrorDialogUtil, LanguageDataProvider}

import scala.scalajs.js
import scala.scalajs.js.{Date, Dictionary}
import scala.xml.Elem
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.09.2017
  */
object SettingsPage extends Layout {
  override val path: String = "settings"

  override def render: Elem = {
    <div id="settings">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-cog" aria-hidden="true"></i>
        {"settings_header".localize}
      </h2>

      <div>
        <h3 class={BoincClientStyle.h4WithoutLine.htmlClass}>
          {"settings_version_group".localize}
        </h3>
        <table class={TableTheme.table.htmlClass}>
          <tbody>
            <tr><td><b>{"verion".localize}</b></td><td>{BuildInfo.version}</td></tr>
            <tr><td><b>{"git_branch".localize}</b></td><td>{BuildInfo.gitCurrentBranch}</td></tr>
            <tr><td><b>{"buid_date".localize}</b></td><td>{new Date(BuildInfo.builtAtMillis).toLocaleDateString()}</td></tr>
            <tr><td><b>{"scala_version".localize}</b></td><td>{BuildInfo.scalaVersion}</td></tr>
          </tbody>
        </table>

        <h3 class={BoincClientStyle.h4WithoutLine.htmlClass}>
          {"settings_language_group".localize}
        </h3>
        <div style="margin-top:25px">
          {
            new LanguageChooser((event, lang_code) => {
              import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
                event.preventDefault()

                NProgress.start()
                LanguageDataProvider
                  .loadLanguage(lang_code)
                  .foreach(_ => {
                    Locale.save(lang_code)

                    // Force complete page re-render
                    LayoutManager.init()
                    this.beforeRender(null)

                    AppRouter.router.updatePageLinks()
                    NProgress.done(true)
                  })
              }).component
          }
        </div>
      </div>
    </div>
  }

  override def after(): Unit = {
    NProgress.done(true)
  }

  override def beforeRender(params: Dictionary[String]): Unit = {
    SettingsTopNavigation.render(Some(""))
    DashboardMenu.selectByMenuId("settings")
  }

}
