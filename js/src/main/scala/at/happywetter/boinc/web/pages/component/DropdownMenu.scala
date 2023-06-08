package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.css.definitions.components.{DropDownMenuStyle => Style}
import mhtml.Rx

import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
class DropdownMenu(text: Elem, elements: Rx[List[Elem]], dropdownStyle: String = ""):

  val component: Elem =
    <div class={Style.dropdown.htmlClass}>
      <a class={Style.button.htmlClass}>{text}</a>
      <div class={Style.dropdownContent.htmlClass} style={dropdownStyle}>{elements}</div>
    </div>
