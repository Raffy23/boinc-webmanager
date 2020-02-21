package at.happywetter.boinc.web.css

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object CSSIdentifier {
  def apply(name: String)(implicit prefix: String): CSSIdentifier =
    if (name.nonEmpty) new CSSIdentifier(prefix + "-" + name)
    else new CSSIdentifier(prefix)
}

class CSSIdentifier(val name: String) extends AnyVal {
  def cssName: String   = "." + GlobalRegistry._getCSSName(name)
  def htmlClass: String = GlobalRegistry._getCSSName(name)
}
