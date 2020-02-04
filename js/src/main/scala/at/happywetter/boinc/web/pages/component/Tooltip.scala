package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.css.definitions.components.{Tooltip => Style}
import at.happywetter.boinc.web.css.CSSIdentifier
import mhtml.{Rx, Var}

import scala.language.postfixOps
import scala.xml.{Elem, Node}
import at.happywetter.boinc.web.util.I18N._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object Tooltip {

  def warningTriangle(label: String): Tooltip =
    new Tooltip(
      Var(label.localize),
      <i class={"fa fa-exclamation-triangle"} aria-hidden="true"></i>,
      style = Some(Style.errorIcon)
    )

  def loadingSpinner(label: String): Tooltip =
    new Tooltip(
      Var(label.localize),
      <i class={"fa fa-spinner fa-pulse"} aria-hidden="true"></i>,
      style = Some(Style.loadingIcon)
    )

}
class Tooltip(text: Rx[String], parent: Elem, textOrientation: CSSIdentifier = Style.topText,
              tooltipId: Option[String] = None, val style: Option[CSSIdentifier] = None) {

  val component: Elem = {
    <div class={Seq(Some(Style.tooltip.htmlClass), style.map(_.htmlClass)).flatten.mkString(" ")}>
      <span class={Style.tooltipText.htmlClass + " " + textOrientation.htmlClass}>{text}</span>
      {parent}
    </div>
  }

  def toXML: Node = component

}
