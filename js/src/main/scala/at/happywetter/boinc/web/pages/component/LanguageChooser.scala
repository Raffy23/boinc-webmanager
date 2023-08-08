package at.happywetter.boinc.web.pages.component

import org.scalajs.dom.Event
import scala.xml.Elem

import at.happywetter.boinc.web.util.I18N.{Locale, _}
import at.happywetter.boinc.web.util.LanguageDataProvider

import mhtml.Var

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.09.2017
  */
class LanguageChooser(langChangeAction: (Event, String) => Unit, left_prop: Int = 0):

  private val languages = Var(LanguageDataProvider.available.toList)
  private val selectedLang = Var(
    LanguageDataProvider.available.find { case (c, _, _) => c == Locale.current }.get
  )

  val component: Elem = new DropdownMenu(
    <span>
      {"login_lang_chooser".localize}
      {
      selectedLang.map { case (_, name, icon) =>
        <span>
              <span style="padding-right:10px">
                <span class={s"flag-icon flag-icon-${icon}"}></span>
              </span>
              {name}
            </span>
      }
    }
    </span>,
    languages.map(_.map { case (lang_code, lang_name, icon) =>
      <a href="#change-language" onclick={
        (event: Event) => {
          selectedLang.update(_ => (lang_code, lang_name, icon))
          langChangeAction(event, lang_code)
        }
      }>
        <span style="padding-right:10px">
          <span class={s"flag-icon flag-icon-${icon}"}></span>
        </span>
        {lang_name}
      </a>
    }),
    if (left_prop != 0) s"left:${left_prop}px" else ""
  ).component
