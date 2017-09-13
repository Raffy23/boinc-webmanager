package at.happywetter.boinc.web.pages
import at.happywetter.boinc.BuildInfo
import at.happywetter.boinc.web.boincclient.{ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.component.{DashboardMenu, LanguageChooser}
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, LoginPageLocation}
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager, NProgress}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.LanguageDataProvider
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.{Date, Dictionary}
import scalatags.JsDom
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.09.2017
  */
object SettingsPage extends Layout {
  override val path: String = "settings"

  override val staticComponent: Option[JsDom.TypedTag[HTMLElement]] =  None

  override def render: Option[JsDom.TypedTag[HTMLElement]] = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._

    Some(
      div(
        DashboardMenu.component.render,
        div(id := "client-container", style := "margin-left:218px",
          div( id := "settings",

            h2(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-cog"), "settings_header".localize),

            div(
              h3(BoincClientLayout.Style.h4_without_line, "settings_version_group".localize),
              table(TableTheme.table,
                tbody(
                  tr(td(b("verion".localize)), td(BuildInfo.version)),
                  tr(td(b("git_branch".localize)), td(BuildInfo.gitCurrentBranch)),
                  tr(td(b("buid_date".localize)), td(new Date(BuildInfo.builtAtMillis).toLocaleDateString())),
                  tr(td(b("scala_version".localize)), td(BuildInfo.scalaVersion))
                )
              )
            ),

            h3(BoincClientLayout.Style.h4_without_line, "settings_language_group".localize),
            div( style := "margin-top: 25px",
              new LanguageChooser((event, lang_code) => {
                event.preventDefault()

                NProgress.start()
                LanguageDataProvider
                  .loadLanguage(lang_code)
                  .foreach(_ => {
                    Locale.save(lang_code)

                    LayoutManager.render(this)
                    this.beforeRender(null)
                    NProgress.done(true)
                  })
              }).component.render()
            )
          )
        )
      )
    )
  }

  override val routerHook: Option[Hook] = Some(new Hook {
    override def already(): Unit = {
      LayoutManager.render(SettingsPage.this)
    }

    override def before(done: js.Function0[Unit]): Unit = {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

      AuthClient.tryLogin.foreach {
        case true => done()
        case false => AppRouter.navigate(LoginPageLocation)
      }
    }

    override def leave(): Unit = {}

    override def after(): Unit = {}
  })

  override def beforeRender(params: Dictionary[String]): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    ClientManager.readClients().map(clients => {
      DashboardMenu.removeMenuReferences("boinc-client-entry")
      clients.foreach(client =>
        DashboardMenu.addMenu(s"${AppRouter.href(DashboardLocation)}/$client",client, Some("boinc-client-entry"))
      )

      AppRouter.router.updatePageLinks()
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }

  override def onRender(): Unit = {
    DashboardMenu.selectByReference("settings")
  }
}
