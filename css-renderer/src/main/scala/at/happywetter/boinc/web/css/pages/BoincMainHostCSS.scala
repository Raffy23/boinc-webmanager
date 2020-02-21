package at.happywetter.boinc.web.css.pages

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincMainHostCSS extends StyleSheet.Standalone {
  import at.happywetter.boinc.web.css.definitions.pages.BoincMainHostStyle._
  import dsl._

  table.cssName - (
    unsafeChild("tbody>tr>td:first-child")(
      width(200 px)
    ),

    unsafeChild("tbody>tr>td")(
      padding(8 px).important
    )
  )
}
