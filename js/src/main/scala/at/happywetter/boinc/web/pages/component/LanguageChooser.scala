package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.util.I18N.{Locale, _}
import at.happywetter.boinc.web.util.LanguageDataProvider
import mhtml.Var
import org.scalajs.dom.Event

import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.09.2017
  */
class LanguageChooser(langChangeAction: (Event, String) => Unit, left_prop: Int = 0) {

  private val imgStyle = "height:2em;vertical-align:middle;margin-left:6px"
  private val languages = Var(LanguageDataProvider.available.toList)
  private val selectedLang = Var(
    LanguageDataProvider.available.find{ case (c,_,_) => c == Locale.current}.get
  )

  val component = new DropdownMenu(
    <span>
      {"login_lang_chooser".localize}
      {
        selectedLang.map{
          case (_, name, icon) =>
            <span>
              <img src={s"/files/images/$icon"} alt={name} style={imgStyle}></img>
              {name}
            </span>
        }
      }
    </span>,
    languages.map(_.map{ case (lang_code, lang_name, icon) =>
      <a href="#change-language" onclick={ (event: Event) => {
        selectedLang.update(_ => (lang_code, lang_name, icon))
        langChangeAction(event, lang_code)
      }}>
        <img src={s"/files/images/$icon"} alt={lang_name} style={imgStyle} >
        </img>
        {lang_name}
      </a>
    }),
    if (left_prop != 0) s"left:${left_prop}px" else ""
  ).component

}
