package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.util.I18N.{Locale, _}
import at.happywetter.boinc.web.util.LanguageDataProvider
import org.scalajs.dom.Event

import scalatags.JsDom.all._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.09.2017
  */
class LanguageChooser(langChangeAction: (Event, String) => Unit, left_prop: Int = 0) {

  val component = new DropdownMenu(
    List(
      "login_lang_chooser".localize,
      LanguageDataProvider.available
        .find{ case (c,_,_) => c == Locale.current}
        .map{ case (lang_code, lang_name, lang_icon) => img(src := s"/files/images/$lang_icon", alt := lang_name, style := "height:2em;vertical-align:middle;margin-left:6px")}
        .get),
    LanguageDataProvider.available.map{ case (lang_code, lang_name, icon) =>
      a(href := "#change-language",
        img(src := s"/files/images/$icon", alt := lang_name, style := "height:2em;vertical-align:middle;margin-right:6px"), lang_name,
        onclick := { (event: Event) => langChangeAction(event, lang_code) }
      )
    }.toList,
    if (left_prop != 0) s"left:${left_prop}px" else ""
  )

}
