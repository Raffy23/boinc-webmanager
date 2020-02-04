package at.happywetter.boinc.web.css

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
trait StyleDefinitions {

  protected[this] implicit val prefix: String

  private[css] def styles: Seq[CSSIdentifier]

}
